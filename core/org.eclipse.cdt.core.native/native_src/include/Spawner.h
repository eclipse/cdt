/*******************************************************************************
 * Copyright (c) 2002, 2020 QNX Software Systems and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     QNX Software Systems - initial API and implementation
 *
 *  Spawner.h
 *
 *  This is a part of JNI implementation of spawner 
 *******************************************************************************/
/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class org_eclipse_cdt_utils_spawner_Spawner */

#ifndef _Included_org_eclipse_cdt_utils_spawner_Spawner
#define _Included_org_eclipse_cdt_utils_spawner_Spawner
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     org_eclipse_cdt_utils_spawner_Spawner
 * Method:    exec0
 * Signature: ([Ljava/lang/String;[Ljava/lang/String;Ljava/lang/String;[Lorg/eclipse/cdt/utils/spawner/Spawner/IChannel;)I
 */
JNIEXPORT jint JNICALL Java_org_eclipse_cdt_utils_spawner_Spawner_exec0
  (JNIEnv *, jobject, jobjectArray, jobjectArray, jstring, jobjectArray);

/*
 * Class:     org_eclipse_cdt_utils_spawner_Spawner
 * Method:    exec1
 * Signature: ([Ljava/lang/String;[Ljava/lang/String;Ljava/lang/String;)I
 */
JNIEXPORT jint JNICALL Java_org_eclipse_cdt_utils_spawner_Spawner_exec1
  (JNIEnv *, jobject, jobjectArray, jobjectArray, jstring);

/*
 * Class:     org_eclipse_cdt_utils_spawner_Spawner
 * Method:    exec2
 * Signature: ([Ljava/lang/String;[Ljava/lang/String;Ljava/lang/String;[Lorg/eclipse/cdt/utils/spawner/Spawner/IChannel;Ljava/lang/String;IZ)I
 */
JNIEXPORT jint JNICALL Java_org_eclipse_cdt_utils_spawner_Spawner_exec2
  (JNIEnv *, jobject, jobjectArray, jobjectArray, jstring, jobjectArray, jstring, jint, jboolean);

/*
 * Class:     org_eclipse_cdt_utils_spawner_Spawner
 * Method:    raise
 * Signature: (II)I
 */
JNIEXPORT jint JNICALL Java_org_eclipse_cdt_utils_spawner_Spawner_raise
  (JNIEnv *, jobject, jint, jint);

/*
 * Class:     org_eclipse_cdt_utils_spawner_Spawner
 * Method:    waitFor
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_org_eclipse_cdt_utils_spawner_Spawner_waitFor
  (JNIEnv *, jobject, jint);

#ifdef __cplusplus
}
#endif
#endif
