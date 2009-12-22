#include "awt_FontMetrics.h"
#include "Font.h"
#include <jni_util.h>
#include <debug_util.h>
#include <interface/Font.h>
#include <math.h>
#include <Rect.h>
#include <limits.h>

void
AwtFontMetrics::makeMetrics(JNIEnv * env, jobject metrics)
{
	jobject jfont = env->GetObjectField(metrics, font_ID);
	BFont font = Font::GetFont(env, jfont);

	font_height height;
	font.GetHeight(&height);
	float lineHeight = height.ascent + height.descent + height.leading;
	env->SetIntField(metrics, leading_ID, (int)ceil(height.leading));
	env->SetIntField(metrics, ascent_ID, (int)ceil(height.ascent));
	env->SetIntField(metrics, descent_ID, (int)ceil(height.descent));
	env->SetIntField(metrics, height_ID, (int)ceil(lineHeight));

	float maxAscent = height.ascent;
	float maxDescent = height.descent;

	// widths[]
	jintArray jwidths = env->NewIntArray(256);
	jsize widthsCount = env->GetArrayLength(jwidths);
	jint * widths = env->GetIntArrayElements(jwidths, 0);
	float bbTop = INT_MAX, bbBottom = -INT_MAX, maxAdvance = -INT_MAX;
	for (int i = 0 ; i < widthsCount ; i++) {
		char c = (char)i;

		BRect boundingBoxes;
		font.GetBoundingBoxesAsGlyphs(&c, 1, B_SCREEN_METRIC, &boundingBoxes);
		if (boundingBoxes.top < bbTop) bbTop = boundingBoxes.top;
		if (boundingBoxes.bottom > bbBottom) bbBottom = boundingBoxes.bottom;

		float width = font.StringWidth(&c, 1);
		jint jwidth = (int)ceil(width);
		if (jwidth > maxAdvance) maxAdvance = jwidth;
		widths[i] = jwidth;
	}
	env->SetObjectField(metrics, widths_ID, jwidths);
	env->ReleaseIntArrayElements(jwidths, widths, 0);

	if (-bbTop > maxAscent) maxAscent = -bbTop;
	if (bbBottom > maxDescent) maxDescent = bbBottom;
	float maxHeight = maxAscent + maxDescent + height.leading;

	BRect boundingBox = font.BoundingBox();
	float bbWidth = boundingBox.Width() * font.Size();
	float bbHeight = boundingBox.Height() * font.Size();
	if (bbWidth > maxAdvance) maxAdvance = bbWidth;
	if (bbHeight > maxHeight) maxHeight = bbHeight;

	env->SetIntField(metrics, maxAscent_ID, (int)ceil(maxAscent));
	env->SetIntField(metrics, maxDescent_ID, (int)ceil(maxDescent));
	env->SetIntField(metrics, maxHeight_ID, (int)ceil(maxHeight));
	env->SetIntField(metrics, maxAdvance_ID, (int)ceil(maxAdvance));

    env->DeleteLocalRef(jfont);
    env->DeleteLocalRef(jwidths);
}

// #pragma mark -

jfieldID AwtFontMetrics::font_ID = NULL;

#include "java_awt_FontMetrics.h"

/*
 * Class:     java_awt_FontMetrics
 * Method:    initIDs
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_java_awt_FontMetrics_initIDs
  (JNIEnv * env, jclass klass)
{
	AwtFontMetrics::font_ID = env->GetFieldID(klass, "font", "Ljava/awt/Font;");
	DASSERT(AwtFontMetrics::font_ID != NULL);
}


// #pragma mark -

jfieldID AwtFontMetrics::widths_ID = NULL;
jfieldID AwtFontMetrics::ascent_ID = NULL;
jfieldID AwtFontMetrics::descent_ID = NULL;
jfieldID AwtFontMetrics::leading_ID = NULL;
jfieldID AwtFontMetrics::height_ID = NULL;
jfieldID AwtFontMetrics::maxAscent_ID = NULL;
jfieldID AwtFontMetrics::maxDescent_ID = NULL;
jfieldID AwtFontMetrics::maxHeight_ID = NULL;
jfieldID AwtFontMetrics::maxAdvance_ID = NULL;

#include "sun_awt_haiku_BFontMetrics.h"

/*
 * Class:     sun_awt_haiku_BFontMetrics
 * Method:    initIDs
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_sun_awt_haiku_BFontMetrics_initIDs
  (JNIEnv * env, jclass klass)
{
	AwtFontMetrics::widths_ID = env->GetFieldID(klass, "widths", "[I");
	AwtFontMetrics::ascent_ID = env->GetFieldID(klass, "ascent", "I");
	AwtFontMetrics::descent_ID = env->GetFieldID(klass, "descent", "I");
	AwtFontMetrics::leading_ID = env->GetFieldID(klass, "leading", "I");
	AwtFontMetrics::height_ID = env->GetFieldID(klass, "height", "I");
	AwtFontMetrics::maxAscent_ID = env->GetFieldID(klass, "maxAscent", "I");
	AwtFontMetrics::maxDescent_ID = env->GetFieldID(klass, "maxDescent", "I");
	AwtFontMetrics::maxHeight_ID = env->GetFieldID(klass, "maxHeight", "I");
	AwtFontMetrics::maxAdvance_ID = env->GetFieldID(klass, "maxAdvance", "I");
	DASSERT(AwtFontMetrics::widths_ID != NULL);
	DASSERT(AwtFontMetrics::ascent_ID != NULL);
	DASSERT(AwtFontMetrics::descent_ID != NULL);
	DASSERT(AwtFontMetrics::leading_ID != NULL);
	DASSERT(AwtFontMetrics::height_ID != NULL);
	DASSERT(AwtFontMetrics::maxAscent_ID != NULL);
	DASSERT(AwtFontMetrics::maxDescent_ID != NULL);
	DASSERT(AwtFontMetrics::maxHeight_ID != NULL);
	DASSERT(AwtFontMetrics::maxAdvance_ID != NULL);
}


/*
 * Class:     sun_awt_haiku_BFontMetrics
 * Method:    _init
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_sun_awt_haiku_BFontMetrics__1init
  (JNIEnv * env, jobject self)
{
	AwtFontMetrics::makeMetrics(env, self);
}


/*
 * Class:     sun_awt_haiku_BFontMetrics
 * Method:    _stringWidth
 * Signature: (Ljava/lang/String;)I
 */
JNIEXPORT jint JNICALL Java_sun_awt_haiku_BFontMetrics__1stringWidth
  (JNIEnv * env, jobject self, jstring string)
{
	jobject jfont = env->GetObjectField(self, AwtFontMetrics::font_ID);
	BFont font = Font::GetFont(env, jfont);
	const char * cstring = env->GetStringUTFChars(string, NULL);
	jint width = (jint)ceil(font.StringWidth(cstring));
	env->ReleaseStringUTFChars(string, cstring);
	return width;
}


/*
 * Class:     sun_awt_haiku_BFontMetrics
 * Method:    _charsWidth
 * Signature: ([CII)I
 */
JNIEXPORT jint JNICALL Java_sun_awt_haiku_BFontMetrics__1charsWidth
  (JNIEnv * env, jobject self, jcharArray data, jint offset, jint length)
{
	jobject jfont = env->GetObjectField(self, AwtFontMetrics::font_ID);
	BFont font = Font::GetFont(env, jfont);
	jchar cstring[length];
	env->GetCharArrayRegion(data, offset, length, cstring);
	jint width = (jint)ceil(font.StringWidth((const char*)cstring, length));
	return width;
}


/*
 * Class:     sun_awt_haiku_BFontMetrics
 * Method:    _bytesWidth
 * Signature: ([BII)I
 */
JNIEXPORT jint JNICALL Java_sun_awt_haiku_BFontMetrics__1bytesWidth
  (JNIEnv * env, jobject self, jbyteArray data, jint offset, jint length)
{
	jobject jfont = env->GetObjectField(self, AwtFontMetrics::font_ID);
	BFont font = Font::GetFont(env, jfont);
	jbyte cstring[length];
	env->GetByteArrayRegion(data, offset, length, cstring);
	jint width = (jint)ceil(font.StringWidth((const char*)cstring, length));
	return width;
}

