/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class org_eclipse_cdt_utils_pty_PTY */

#ifndef _Included_org_eclipse_cdt_utils_pty_PTY
#define _Included_org_eclipse_cdt_utils_pty_PTY
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     org_eclipse_cdt_utils_pty_PTY
 * Method:    openMaster
 * Signature: (Z)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_org_eclipse_cdt_utils_pty_PTY_openMaster(JNIEnv *, jobject, jboolean);

/*
 * Class:     org_eclipse_cdt_utils_pty_PTY
 * Method:    change_window_size
 * Signature: (III)I
 */
JNIEXPORT jint JNICALL Java_org_eclipse_cdt_utils_pty_PTY_change_1window_1size(JNIEnv *, jobject, jint, jint, jint);

/*
 * Class:     org_eclipse_cdt_utils_pty_PTY
 * Method:    exec2
 * Signature:
 * ([Ljava/lang/String;[Ljava/lang/String;Ljava/lang/String;[Lorg/eclipse/cdt/utils/spawner/Spawner/IChannel;Ljava/lang/String;IZ)I
 */
JNIEXPORT jint JNICALL Java_org_eclipse_cdt_utils_pty_PTY_exec2(JNIEnv *, jobject, jobjectArray, jobjectArray, jstring,
                                                                jobjectArray, jstring, jint, jboolean);

/*
 * Class:     org_eclipse_cdt_utils_pty_PTY
 * Method:    waitFor
 * Signature: (II)I
 */
JNIEXPORT jint JNICALL Java_org_eclipse_cdt_utils_pty_PTY_waitFor(JNIEnv *, jobject, jint, jint);

#ifdef __cplusplus
}
#endif
#endif
