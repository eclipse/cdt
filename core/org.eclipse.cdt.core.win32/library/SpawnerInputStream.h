/*******************************************************************************
 * Copyright (c) 2002, 2007 QNX Software Systems and others.
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
 *  SpawnerInputStream.h
 *
 *  This is a part of JNI implementation of spawner
 *******************************************************************************/

/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class com_qnx_tools_utils_spawner_SpawnerInputStream */

#ifndef _Included_com_qnx_tools_utils_spawner_SpawnerInputStream
#define _Included_com_qnx_tools_utils_spawner_SpawnerInputStream
#ifdef __cplusplus
extern "C" {
#endif
#undef com_qnx_tools_utils_spawner_SpawnerInputStream_SKIP_BUFFER_SIZE
#define com_qnx_tools_utils_spawner_SpawnerInputStream_SKIP_BUFFER_SIZE 2048L
/* Inaccessible static: skipBuffer */
/*
 * Class:     com_qnx_tools_utils_spawner_SpawnerInputStream
 * Method:    read0
 * Signature: (I[BI)I
 */
JNIEXPORT jint JNICALL Java_com_qnx_tools_utils_spawner_SpawnerInputStream_read0
  (JNIEnv *, jobject, jint, jbyteArray, jint);

/*
 * Class:     com_qnx_tools_utils_spawner_SpawnerInputStream
 * Method:    close0
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_com_qnx_tools_utils_spawner_SpawnerInputStream_close0
  (JNIEnv *, jobject, jint);

#ifdef __cplusplus
}
#endif
#endif
