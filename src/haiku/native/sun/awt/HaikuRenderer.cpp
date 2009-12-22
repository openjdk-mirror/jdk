#include "HaikuRenderer.h"

/*
 * Class:     sun_awt_HaikuRenderer
 * Method:    initIDs
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_sun_awt_HaikuRenderer_initIDs
  (JNIEnv *env, jclass cls)
{
}

/*
 * Class:     sun_awt_HaikuRenderer
 * Method:    _drawLine
 * Signature: (Lsun/java2d/SurfaceData;Lsun/java2d/pipe/Region;Ljava/awt/Composite;IIIII)V
 */
JNIEXPORT void JNICALL Java_sun_awt_HaikuRenderer__1drawLine
  (JNIEnv *env, jobject self, jobject sData, jobject clip, 
  jobject comp, jint color, jint x1, jint y1, jint x2, jint y2)
{
}

/*
 * Class:     sun_awt_HaikuRenderer
 * Method:    _drawRect
 * Signature: (Lsun/java2d/SurfaceData;Lsun/java2d/pipe/Region;Ljava/awt/Composite;IIIII)V
 */
JNIEXPORT void JNICALL Java_sun_awt_HaikuRenderer__1drawRect
  (JNIEnv *env, jobject self, jobject sData, jobject clip,
  jobject comp, jint color, jint x, jint y, jint w, jint h)
{
}

/*
 * Class:     sun_awt_HaikuRenderer
 * Method:    _drawRoundRect
 * Signature: (Lsun/java2d/SurfaceData;Lsun/java2d/pipe/Region;Ljava/awt/Composite;IIIIIII)V
 */
JNIEXPORT void JNICALL Java_sun_awt_HaikuRenderer__1drawRoundRect
  (JNIEnv *env, jobject self, jobject sData, jobject clip, jobject comp,
  jint color, jint x, jint y, jint w, jint h, jint arcW, jint arcH)
{
}

/*
 * Class:     sun_awt_HaikuRenderer
 * Method:    _drawOval
 * Signature: (Lsun/java2d/SurfaceData;Lsun/java2d/pipe/Region;Ljava/awt/Composite;IIIII)V
 */
JNIEXPORT void JNICALL Java_sun_awt_HaikuRenderer__1drawOval
  (JNIEnv *env, jobject self, jobject sData, jobject clip, jobject comp,
  jint color, jint x, jint y, jint w, jint h)
{
}

/*
 * Class:     sun_awt_HaikuRenderer
 * Method:    _drawArc
 * Signature: (Lsun/java2d/SurfaceData;Lsun/java2d/pipe/Region;Ljava/awt/Composite;IIIIIII)V
 */
JNIEXPORT void JNICALL Java_sun_awt_HaikuRenderer__1drawArc
  (JNIEnv *env, jobject self, jobject sData, jobject clip, jobject comp,
  jint color, jint x, jint y, jint w, jint h, jint angleStart, jint angleExtent)
{
}

/*
 * Class:     sun_awt_HaikuRenderer
 * Method:    _drawPoly
 * Signature: (Lsun/java2d/SurfaceData;Lsun/java2d/pipe/Region;Ljava/awt/Composite;III[I[IIZ)V
 */
JNIEXPORT void JNICALL Java_sun_awt_HaikuRenderer__1drawPoly
  (JNIEnv *env, jobject self, jobject sData, jobject clip, jobject comp,
  jint color, jint x, jint y, jintArray xpoints, jintArray ypoints, jint npoints, jboolean closed)
{
}

/*
 * Class:     sun_awt_HaikuRenderer
 * Method:    _fillRect
 * Signature: (Lsun/java2d/SurfaceData;Lsun/java2d/pipe/Region;Ljava/awt/Composite;IIIII)V
 */
JNIEXPORT void JNICALL Java_sun_awt_HaikuRenderer__1fillRect
  (JNIEnv *env, jobject self, jobject sData, jobject clip, jobject comp,
  jint color, jint x, jint y, jint w, jint h)
{
}

/*
 * Class:     sun_awt_HaikuRenderer
 * Method:    _fillRoundRect
 * Signature: (Lsun/java2d/SurfaceData;Lsun/java2d/pipe/Region;Ljava/awt/Composite;IIIIIII)V
 */
JNIEXPORT void JNICALL Java_sun_awt_HaikuRenderer__1fillRoundRect
  (JNIEnv *env, jobject self, jobject sData, jobject clip, jobject comp,
  jint color, jint x, jint y, jint w, jint h, jint arcW, jint arcH)
{
}

/*
 * Class:     sun_awt_HaikuRenderer
 * Method:    _fillOval
 * Signature: (Lsun/java2d/SurfaceData;Lsun/java2d/pipe/Region;Ljava/awt/Composite;IIIII)V
 */
JNIEXPORT void JNICALL Java_sun_awt_HaikuRenderer__1fillOval
  (JNIEnv *env, jobject self, jobject sData, jobject clip, jobject comp,
  jint color, jint x, jint y, jint w, jint h)
{
}

/*
 * Class:     sun_awt_HaikuRenderer
 * Method:    _fillArc
 * Signature: (Lsun/java2d/SurfaceData;Lsun/java2d/pipe/Region;Ljava/awt/Composite;IIIIIII)V
 */
JNIEXPORT void JNICALL Java_sun_awt_HaikuRenderer__1fillArc
  (JNIEnv *env, jobject self, jobject sData, jobject clip, jobject comp,
  jint color, jint x, jint y, jint w, jint h, jint angleStart, jint angleExtent)
{
}

/*
 * Class:     sun_awt_HaikuRenderer
 * Method:    _fillPoly
 * Signature: (Lsun/java2d/SurfaceData;Lsun/java2d/pipe/Region;Ljava/awt/Composite;III[I[II)V
 */
JNIEXPORT void JNICALL Java_sun_awt_HaikuRenderer__1fillPoly
  (JNIEnv *env, jobject self, jobject sData, jobject clip, jobject comp,
  jint color, jint xtrans, jint ytrans, jintArray xpoints, jintArray ypoints, jint npoints)
{
}

/*
 * Class:     sun_awt_HaikuRenderer
 * Method:    _shape
 * Signature: (Lsun/java2d/SurfaceData;Lsun/java2d/pipe/Region;Ljava/awt/Composite;IIILjava/awt/geom/GeneralPath;Z)V
 */
JNIEXPORT void JNICALL Java_sun_awt_HaikuRenderer__1shape
  (JNIEnv *env, jobject self, jobject sData, jobject clip, jobject comp,
  jint color, jint xtrans, jint ytrans, jobject path, jboolean isFilled)
{
}

/*
 * Class:     sun_awt_HaikuRenderer
 * Method:    _copyArea
 * Signature: (Lsun/java2d/SurfaceData;IIIIII)V
 */
JNIEXPORT void JNICALL Java_sun_awt_HaikuRenderer__1copyArea
  (JNIEnv *env, jobject self, jobject sData, jint srcx, jint xrcy, jint dx, jint dy, jint w, jint h)
{
}
