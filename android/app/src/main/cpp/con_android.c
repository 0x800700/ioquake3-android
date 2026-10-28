#include "../../../../../ioq3/code/qcommon/q_shared.h"
#include "../../../../../ioq3/code/qcommon/qcommon.h"
#include "../../../../../ioq3/code/sys/sys_local.h"
#include <android/log.h>
#include <stdio.h>

#define LOG_TAG "ioquake3"

void CON_Shutdown( void ) {
}

void CON_Init( void ) {
}

char *CON_Input( void ) {
    return NULL;
}

void CON_Print( const char *msg ) {
    if ( msg && *msg ) {
        __android_log_print( ANDROID_LOG_INFO, LOG_TAG, "%s", msg );
    }
}
