#include "Font.h"
#include "debug_util.h"
#include <cstdio>

BFont
Font::GetFont(JNIEnv * jenv, jobject jfont)
{
	// get the family name
	jstring jfamily = (jstring)jenv->CallObjectMethod(jfont, getFamily_ID);
	const char * familychars = jenv->GetStringUTFChars(jfamily, NULL);
	font_family family;
	strncpy(family, familychars, B_FONT_FAMILY_LENGTH);
	jenv->ReleaseStringUTFChars(jfamily, familychars);

	// get the face
	int jface = jenv->GetIntField(jfont, style_ID);
	uint16 face = 0;
	if (jface & java_awt_Font_PLAIN) {
		face |= B_REGULAR_FACE;
	}
	if (jface & java_awt_Font_BOLD) {
		face |= B_BOLD_FACE;
	}
	if (jface & java_awt_Font_ITALIC) {
		face |= B_ITALIC_FACE;
	}

	// get the size
	float size = jenv->GetFloatField(jfont, pointSize_ID);

	// Construct the font, using the system fonts for a base.
	// Using the system fonts as a base will cause us to fail
	// in a reasonable fashion on unexpected family names.
	BFont font(be_plain_font);
	if (face & B_BOLD_FACE) {
		font = be_bold_font;
	}
	if (strcasecmp(family, "monospaced") == 0) {
		font = be_fixed_font;
	}
	font.SetFamilyAndFace(family, face);
	font.SetSize(size);

	// last resort: use shear (14 degrees) to simulate italic
	if ((jface & java_awt_Font_ITALIC) && !(font.Face() & B_ITALIC_FACE)) {
		font.SetShear(104);
	}

	return font;
}


// #pragma mark -

jfieldID  Font::style_ID     = NULL;
jfieldID  Font::pointSize_ID = NULL;
jmethodID Font::getFamily_ID = NULL;

extern "C" {

/*
 * Class:     java_awt_Font
 * Method:    initIDs
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_java_awt_Font_initIDs
  (JNIEnv * jenv, jclass jklass)
{
    Font::style_ID     = jenv->GetFieldID(jklass, "style", "I");
    Font::pointSize_ID = jenv->GetFieldID(jklass, "pointSize", "F");
    Font::getFamily_ID = jenv->GetMethodID(jklass, "getFamily_NoClientCode", "()Ljava/lang/String;");

    DASSERT(Font::style_ID     != NULL);
    DASSERT(Font::pointSize_ID != NULL);
    DASSERT(Font::getFamily_ID != NULL);
}

/*
 * Class:     java_awt_Font
 * Method:    pDispose
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_java_awt_Font_pDispose
  (JNIEnv * jenv, jobject jself)
{
	// nothing to do.
}

} /* extern "C" */
