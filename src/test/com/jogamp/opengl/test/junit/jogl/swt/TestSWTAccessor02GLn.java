/**
 * Copyright 2010 JogAmp Community. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 * 
 *    1. Redistributions of source code must retain the above copyright notice, this list of
 *       conditions and the following disclaimer.
 * 
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list
 *       of conditions and the following disclaimer in the documentation and/or other materials
 *       provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY JogAmp Community ``AS IS'' AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL JogAmp Community OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 * The views and conclusions contained in the software and documentation are those of the
 * authors and should not be interpreted as representing official policies, either expressed
 * or implied, of JogAmp Community.
 */
 
package com.jogamp.opengl.test.junit.jogl.swt;

import javax.media.opengl.GL2ES1;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLDrawableFactory;
import javax.media.opengl.GLProfile;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.After;
import org.junit.Test;

import com.jogamp.nativewindow.swt.SWTAccessor;
import com.jogamp.opengl.test.junit.jogl.demos.es1.OneTriangle;
import com.jogamp.opengl.test.junit.util.UITestCase;
import javax.media.nativewindow.AbstractGraphicsDevice;
import javax.media.nativewindow.ProxySurface;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLDrawable;
import org.eclipse.swt.widgets.Canvas;

/**
 * Tests that a basic SWT app can open without crashing under different GL profiles. 
 * <p> 
 * Uses JOGL's SWTAccessor only.
 * </p>
 * @author Wade Walker, et.al.
 */
public class TestSWTAccessor02GLn extends UITestCase {

    static int duration = 250;

    static final int iwidth = 640;
    static final int iheight = 480;

    Display display = null;
    Shell shell = null;
    Composite composite = null;

    @BeforeClass
    public static void startup() {
        System.out.println( "GLProfile " + GLProfile.glAvailabilityToString() );
    }

    @Before
    public void init() {
        SWTAccessor.invoke(true, new Runnable() {
            public void run() {        
                display = new Display();
                Assert.assertNotNull( display );
                shell = new Shell( display );
                Assert.assertNotNull( shell );
                shell.setLayout( new FillLayout() );
                composite = new Composite( shell, SWT.NONE );
                composite.setLayout( new FillLayout() );
                Assert.assertNotNull( composite );
            }});        
    }

    @After
    public void release() {
        Assert.assertNotNull( display );
        Assert.assertNotNull( shell );
        Assert.assertNotNull( composite );
        try {
            SWTAccessor.invoke(true, new Runnable() {
               public void run() {
                composite.dispose();
                shell.dispose();
                display.dispose();
               }});
        }
        catch( Throwable throwable ) {
            throwable.printStackTrace();
            Assume.assumeNoException( throwable );
        }
        display = null;
        shell = null;
        composite = null;
    }
    
    class CanvasCStr implements Runnable {
           Canvas canvas;
           
           public void run() {
               canvas = new Canvas( composite, SWT.NO_BACKGROUND);
           }        
    }
    
    protected void runTestAGL( GLProfile glprofile ) throws InterruptedException {
        GLCapabilities caps = new GLCapabilities(glprofile);
        GLDrawableFactory factory = GLDrawableFactory.getFactory(glprofile);
        
        // need SWT.NO_BACKGROUND to prevent SWT from clearing the window
        // at the wrong times (we use glClear for this instead)
        CanvasCStr canvasCstr = new CanvasCStr();
        
        SWTAccessor.invoke(true, canvasCstr);
        final Canvas canvas = canvasCstr.canvas;        
        Assert.assertNotNull( canvas );
        
        SWTAccessor.setRealized(canvas, true);
        AbstractGraphicsDevice device = SWTAccessor.getDevice(canvas);
        long nativeWindowHandle = SWTAccessor.getWindowHandle(canvas);
        System.err.println("*** device: " + device);
        System.err.println("*** window handle: 0x" + Long.toHexString(nativeWindowHandle));
        
        final SWTUpstreamHook swtUpstreamHook = new SWTUpstreamHook(canvas);
        final ProxySurface proxySurface = factory.createProxySurface(device, 0, nativeWindowHandle, caps, null, swtUpstreamHook);
        Assert.assertNotNull( proxySurface );        
        System.err.println("*** ProxySurface: " + proxySurface);
        final GLDrawable drawable = factory.createGLDrawable(proxySurface);
        Assert.assertNotNull( drawable );
        drawable.setRealized(true);
        System.err.println("*** Drawable: " + drawable);
        Assert.assertTrue( drawable.isRealized() );
        final GLContext glcontext = drawable.createContext(null);
        // trigger native creation ..
        if( GLContext.CONTEXT_NOT_CURRENT < glcontext.makeCurrent() ) {        
            glcontext.release();
        }
        
        final boolean[] sizeMissing = new boolean[] { false };
        
        // fix the viewport when the user resizes the window
        canvas.addListener( SWT.Resize, new Listener() {
            public void handleEvent( Event event ) {
                Rectangle rectangle = canvas.getClientArea();
                boolean glok=false;
                if( GLContext.CONTEXT_NOT_CURRENT < glcontext.makeCurrent() ) {
                    glok=true;
                    GL2ES1 gl = glcontext.getGL().getGL2ES1();
                    OneTriangle.setup( gl, rectangle.width, rectangle.height );
                    glcontext.release();
                } else {
                    sizeMissing[0] = true;
                }
                System.err.println("resize: glok " + glok);
            }
        });

        // draw the triangle when the OS tells us that any part of the window needs drawing
        canvas.addPaintListener( new PaintListener() {
            public void paintControl( PaintEvent paintevent ) {
                Rectangle rectangle = canvas.getClientArea();
                boolean glok=false;
                if( GLContext.CONTEXT_NOT_CURRENT < glcontext.makeCurrent() ) {
                    glok=true;
                    GL2ES1 gl = glcontext.getGL().getGL2ES1();
                    if(sizeMissing[0]) {
                        OneTriangle.setup( gl, rectangle.width, rectangle.height);
                        sizeMissing[0] = false;
                    }
                    OneTriangle.render( gl, rectangle.width, rectangle.height);
                    drawable.swapBuffers();
                    glcontext.release();
                }
                System.err.println("paint: glok " + glok);
            }
        });
        
        shell.setText( getClass().getName() );
        shell.setSize( 640, 480 );
        shell.open();

        long lStartTime = System.currentTimeMillis();
        long lEndTime = lStartTime + duration;
        try {
            while( (System.currentTimeMillis() < lEndTime) && !canvas.isDisposed() ) {
                if( !display.readAndDispatch() ) {
                    // blocks on linux .. display.sleep();
                    Thread.sleep(10);
                }
            }
        } catch( Throwable throwable ) {
            throwable.printStackTrace();
            Assume.assumeNoException( throwable );
        }
        glcontext.destroy();
        drawable.setRealized(false);
        canvas.dispose();
    }
    private static class SWTUpstreamHook implements ProxySurface.UpstreamSurfaceHook, ControlListener {
        private Canvas c;
        Rectangle clientArea;
        public SWTUpstreamHook(Canvas c) {
            this.c = c ;
            this.clientArea = c.getClientArea();
        }
        @Override
        public final void create(ProxySurface s) { /* nop */ }
    
        @Override
        public final void destroy(ProxySurface s) { /* nop */ }
    
        @Override
        public final int getWidth(ProxySurface s) {
            return clientArea.width;
        }    
        @Override
        public final int getHeight(ProxySurface s) {
            return clientArea.width;
        }
        
        @Override
        public void controlResized(final ControlEvent arg0) {
            clientArea = c.getClientArea();
        }
        @Override
        public void controlMoved(ControlEvent e) {
        }
        @Override
        public String toString() {
            final String us_s = null != c ? c.toString() : "nil"; 
            return "SETUpstreamSurfaceHook[upstream: "+us_s+"]";
    }
    };

    @Test
    public void test() throws InterruptedException {
        GLProfile glprofile = GLProfile.getGL2ES1();
        runTestAGL( glprofile );
    }

    static int atoi(String a) {
        int i=0;
        try {
            i = Integer.parseInt(a);
        } catch (Exception ex) { ex.printStackTrace(); }
        return i;
    }

    public static void main(String args[]) {
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                duration = atoi(args[++i]);
            }
        }
        System.out.println("durationPerTest: "+duration);
        org.junit.runner.JUnitCore.main(TestSWTAccessor02GLn.class.getName());
    }
}
