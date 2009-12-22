#include "sun_awt_HaikuGraphicsEnvironment.h"
#include "Utils.h"
#include <Directory.h>
#include <FindDirectory.h>
#include <Entry.h>
#include <Path.h>
#include <Font.h>
#include <String.h>
#include <File.h>

static void
registerFontsWithPlatform(const char * sourcePath)
{
	BPath destinationPath;
	if (find_directory(B_USER_FONTS_DIRECTORY, &destinationPath, true) == B_OK) {
		destinationPath.Append("ttfonts");
		BDirectory destination(destinationPath.Path());
		if (destination.InitCheck() != B_OK) {
			return;
		}
		bool rescan = false;
		BDirectory source(sourcePath);
		BEntry fontEntry;
		while (source.GetNextEntry(&fontEntry) == B_OK) {
			char fontName[MAXPATHLEN];
			if (fontEntry.GetName(fontName) != B_OK) {
				continue;
			}
			BFile input(&fontEntry, B_READ_ONLY);
			if (input.InitCheck() != B_OK) {
				continue;
			}
			BFile output;
			if (destination.CreateFile(fontName, &output, true) != B_OK) {
				continue;
			}
			ssize_t count;
			char buffer[4096];
			while ((count = input.Read((void *)buffer, sizeof(buffer))) > 0) {
				output.Write(buffer, count);
			}
			rescan = true;
		}
		if (rescan) {
			update_font_families(false);
		}
	}
}

/*
 * Class:     sun_awt_HaikuGraphicsEnvironment
 * Method:    _registerFontsWithPlatform
 * Signature: (Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_sun_awt_HaikuGraphicsEnvironment__1registerFontsWithPlatform
  (JNIEnv * jenv, jobject jpeer, jstring jpath)
{
	const char * path = UseString(jenv, jpath);
	if (path != NULL) {
		registerFontsWithPlatform(path);
		ReleaseString(jenv, jpath, path);
	}
}

