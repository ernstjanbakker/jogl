#! /bin/sh

SDIR=`dirname $0` 

if [ -e $SDIR/../../../gluegen/make/scripts/setenv-build-jogl-x86_64.sh ] ; then
    . $SDIR/../../../gluegen/make/scripts/setenv-build-jogl-x86_64.sh
fi

if [ "$1" = "-libdir" ] ; then
    shift
    if [ -z "$1" ] ; then
        echo libdir argument missing
        print_usage
        exit
    fi
    CUSTOMLIBDIR="-Dcustom.libdir=$1"
    shift
fi

#    -Dc.compiler.debug=true \

#    -Dgluegen.cpptasks.detected.os=true \
#    -DisUnix=true \
#    -DisLinux=true \
#    -DisLinuxAMD64=true \
#    -DisX11=true \
#
#    -Dtarget.sourcelevel=1.6 \
#    -Dtarget.targetlevel=1.6 \
#    -Dtarget.rt.jar=/opt-share/jre1.6.0_30/lib/rt.jar \
#
#    -Dsetup.addNativeOpenMAX=true \
#    -Dsetup.addNativeKD=true \


#LD_LIBRARY_PATH=/opt-linux-x86_64/mesa-7.8.1/lib64
#export LD_LIBRARY_PATH

LOGF=make.jogl.b202_gl44.linux-x86_64.log
rm -f $LOGF

# export LIBGL_DRIVERS_PATH=/usr/lib/fglrx/dri:/usr/lib32/fglrx/dri
# export LIBGL_DEBUG=verbose
echo LIBXCB_ALLOW_SLOPPY_LOCK: $LIBXCB_ALLOW_SLOPPY_LOCK 2>&1 | tee -a $LOGF
echo LIBGL_DRIVERS_PATH: $LIBGL_DRIVERS_PATH 2>&1 | tee -a $LOGF
echo LIBGL_DEBUG: $LIBGL_DEBUG 2>&1 | tee -a $LOGF

#    -Dc.compiler.debug=true \
#    -Djavacdebug="true" \
#    -Djavacdebuglevel="source,lines,vars" \

# BUILD_ARCHIVE=true \
ant  \
    $CUSTOMLIBDIR \
    -Dtarget.sourcelevel=1.6 \
    -Dtarget.targetlevel=1.6 \
    -Dtarget.rt.jar=/opt-share/jre1.6.0_30/lib/rt.jar \
    -Drootrel.build=build-b202_gl44-x86_64 \
    $* 2>&1 | tee -a $LOGF
