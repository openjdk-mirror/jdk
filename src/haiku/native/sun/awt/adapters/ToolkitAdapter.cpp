#include "ToolkitAdapter.h"

#include "Application.h"
#include "ComponentAdapter.h"
#include "KeyboardFocusManager.h"
#include "MenuComponentAdapter.h"

#include "java_awt_Event.h"
#include "java_awt_event_KeyEvent.h"
#include "java_awt_Toolkit.h"

#include "jni.h"

#include <InterfaceDefs.h>
#include <Screen.h>
#include <sys/utsname.h>

ToolkitAdapter* ToolkitAdapter::fInstance = NULL;
sem_id ToolkitAdapter::fSemAppRun = B_NO_INIT;

ToolkitAdapter::ToolkitAdapter() {
	fEmbedded = be_app_messenger.IsValid();
	fPeer = NULL;
	fSemAppRun = create_sem(0, "awt_application_run");
	fThidApp = B_NO_INIT;
}

ToolkitAdapter::~ToolkitAdapter() {
	if (! fEmbedded) {
		delete be_app;
	}
	delete_sem(fSemAppRun);
	
	delete KeyboardFocusManager::GetInstance();
	
	fEmbedded = false;
}

/**
 * Gets the instance of the ToolkitAdapter, creates one if it
 * dosen't exist.
 */
ToolkitAdapter* ToolkitAdapter::GetInstance() {
	if (ToolkitAdapter::fInstance == NULL) {
		ToolkitAdapter::fInstance = new ToolkitAdapter();
	}
	return ToolkitAdapter::fInstance;
}

/**
 * Dumps some system info to Standard Output.
 */
void ToolkitAdapter::printSystemVersion() {
	utsname osinfo;
	uname(&osinfo);
	
	fprintf(stdout, "%s %s %s\n", osinfo.sysname, osinfo.release, osinfo.version);
}

/**
 * Creates a BApplication if one is required.
 */
void ToolkitAdapter::createApplicationThread() {
	if (! fEmbedded) {
		fThidApp = spawn_thread(ToolkitAdapter::java_app_main_thread, "java", B_NORMAL_PRIORITY, NULL);
		resume_thread(fThidApp);
	}
}

/**
 * Sets the peer Toolkit java object
 */
void ToolkitAdapter::setPeer(JNIEnv *jenv, jobject jpeer) {
	if (fPeer != NULL) {
		jenv->DeleteGlobalRef(fPeer);
	}
	
	if (jpeer != NULL) {
		fPeer = jenv->NewGlobalRef(jpeer);
	}
}

/**
 * If we're not running embedded, this allows the Application
 * to Run();
 */
void ToolkitAdapter::runApplicationThread() {
	release_sem(fSemAppRun);
}

/**
 * Calls Quit() on the Application when not in embedded mode.
 */
void ToolkitAdapter::haltApplication() {
	if (! fEmbedded) {
		if (be_app->LockLooper()) {
			be_app->Quit();
			be_app->UnlockLooper();
		}
		
		status_t result;
		wait_for_thread(fThidApp, &result);
	}
	return;
}

/**
 * Gets the dpi of the current BScreen.
 */
jint ToolkitAdapter::getScreenResolution() {
	return 72;
}

/**
 * Gets the width of the current workspace.
 */
jint ToolkitAdapter::getScreenWidth() {
	return (BScreen().Frame().IntegerWidth() + 1);
}

/**
 * Gets the height of the current workspace.
 */
jint ToolkitAdapter::getScreenHeight() {
	return (BScreen().Frame().IntegerHeight() + 1);
}

/**
 * Gets the default (user-set) font size for the
 * given type of standard font.
 */
jint ToolkitAdapter::getFontSize(jint type) {
	const BFont *font;
	switch(type) {
		case sun_awt_haiku_BToolkit_B_BOLD_FONT:
			font = be_bold_font;
			break;
		case sun_awt_haiku_BToolkit_B_FIXED_FONT:
			font = be_fixed_font;
			break;
		case sun_awt_haiku_BToolkit_B_MENU_FONT:
			menu_info minfo;
			if (get_menu_info(&minfo) == B_OK) {
				return (jint)minfo.font_size;
			} // fallback to plain font on a failure
		case sun_awt_haiku_BToolkit_B_PLAIN_FONT:
		default:
			font = be_plain_font;
	}
	return (jint)font->Size();
}

/**
 * Gets the default (user-set) font style for the
 * given type of standard font.
 */
jint ToolkitAdapter::getFontStyle(jint type) {
	const BFont *font;
	switch(type) {
		case sun_awt_haiku_BToolkit_B_BOLD_FONT:
			font = be_bold_font;
			break;
		case sun_awt_haiku_BToolkit_B_FIXED_FONT:
			font = be_fixed_font;
			break;
		case sun_awt_haiku_BToolkit_B_MENU_FONT:
			menu_info minfo;
			if (get_menu_info(&minfo) == B_OK) {
				BFont f = new BFont(be_plain_font);
				f.SetFamilyAndStyle(minfo.f_family, minfo.f_style);
				f.SetSize(minfo.font_size);
				break;
			} // fallback to the plain font on a failure.
		case sun_awt_haiku_BToolkit_B_PLAIN_FONT:
		default:
			font = be_plain_font;
	}
	
	// Compose a java style from our face settings.
	uint16 face = font->Face();
	jint style = 0;
	if (face & B_REGULAR_FACE) {
		return java_awt_Font_PLAIN;
	} else if (face & B_ITALIC_FACE) {
		style = style | java_awt_Font_ITALIC;
	} else if (face & B_BOLD_FACE) {
		style = style | java_awt_Font_BOLD;
	}
	return style;
}

/**
 * Sync's all Windows owned by the Application consecutively.
 * This will sync non-java windows in an embedded situation.
 */
void ToolkitAdapter::sync() {
	BWindow *window;
	int32 i = 0;
	while (window = be_app->WindowAt(i++)) {
		window->Sync();
	}
	return;
}

/**
 * Gets the keyboard shortcut mask.
 */
jint ToolkitAdapter::getMenuShortcutKeyMask() {
	key_map *keys;
	char *chars;
	get_key_map(&keys, &chars);
	jint shortcutKey ;
	switch (keys->left_command_key) {
	case 0x4b: case 0x56:
		shortcutKey = java_awt_Event_SHIFT_MASK;
		break;
	case 0x60:
		if (keys->right_control_key == 0) {
			shortcutKey = java_awt_Event_META_MASK;
			break;
		}
	case 0x5c: // fall through
		shortcutKey = java_awt_Event_CTRL_MASK;
		break;
	case 0x66: case 0x67:
		shortcutKey = java_awt_Event_META_MASK;
		break;
	case 0x5d: case 0x5f: default:
		shortcutKey = java_awt_Event_ALT_MASK;
		break;
	}
	free(keys);
	free(chars);
	return shortcutKey;
}

/**
 * Determine if a keyboard lock is on or off.
 */
jboolean ToolkitAdapter::isKeyLocked(jint key) {
	key_info info;
	if (get_key_info(&info) != B_OK) {
		return false;
	}
	key_map *keys;
	char *chars;
	get_key_map(&keys, &chars);
	bool down = false;
	switch (key) {
	case java_awt_event_KeyEvent_VK_CAPS_LOCK:
		down = key_down(info.key_states, keys->caps_key);
		break;
	case java_awt_event_KeyEvent_VK_NUM_LOCK:
		down = key_down(info.key_states, keys->num_key);
		break;
	case java_awt_event_KeyEvent_VK_SCROLL_LOCK:
		down = key_down(info.key_states, keys->scroll_key);
		break;
	default:
		break;
	}
	free(keys);
	free(chars);
	return down;
}

/**
 * Sets a key lock.
 */
void ToolkitAdapter::setKeyLocked(jint key, jboolean on) {
	key_info info;
	if (get_key_info(&info) != B_OK) {
		// failed to got the current state, so we clear all the other locks
		switch (key) {
		case java_awt_event_KeyEvent_VK_CAPS_LOCK:
			set_keyboard_locks((on ? B_CAPS_LOCK : 0));
			break;
		case java_awt_event_KeyEvent_VK_NUM_LOCK:
			set_keyboard_locks((on ? B_NUM_LOCK : 0));
			break;
		case java_awt_event_KeyEvent_VK_SCROLL_LOCK:
			set_keyboard_locks((on ? B_SCROLL_LOCK : 0));
			break;
		default:
			break;
		}
		return;
	}
	
	// we got the current state, so just affect the requested keyboard lock
	key_map *keys;
	char *chars;
	get_key_map(&keys, &chars);
	uint32 modifiers = 0;
	switch (key) {
	case java_awt_event_KeyEvent_VK_CAPS_LOCK:
		modifiers = (on ? B_CAPS_LOCK : 0)
			| (key_down(info.key_states, keys->num_key) ? B_NUM_LOCK : 0)
			| (key_down(info.key_states, keys->scroll_key) ? B_SCROLL_LOCK : 0);
		break;
	case java_awt_event_KeyEvent_VK_NUM_LOCK:
		modifiers = (on ? B_NUM_LOCK : 0)
			| (key_down(info.key_states, keys->scroll_key) ? B_SCROLL_LOCK : 0)
			| (key_down(info.key_states, keys->caps_key) ? B_CAPS_LOCK : 0);
		break;
	case java_awt_event_KeyEvent_VK_SCROLL_LOCK:
		modifiers = (on ? B_SCROLL_LOCK : 0)
			| (key_down(info.key_states, keys->caps_key) ? B_CAPS_LOCK : 0)
			| (key_down(info.key_states, keys->num_key) ? B_NUM_LOCK : 0);
		break;
	default:
		free(keys);
		free(chars);
		return;
		break;
	}
	set_keyboard_locks(modifiers);
	free(keys);
	free(chars);
}

/**
 * Gets the delay between mouse clicks.
 */
jint ToolkitAdapter::getMulticlickTime() {
	bigtime_t time;
	if (get_click_speed(&time) == B_OK) {
		return (int32)time;
	} else {
		return 1000;
	}
}

/* Function that carries the Application's message loop */
int32 ToolkitAdapter::java_app_main_thread(void *arg) {
	JNIEnv *jenv;
	jvm->AttachCurrentThreadAsDaemon((void**)&jenv, NULL);
	
	Application *app = new Application();
	
	// Blocks until the semaphore is released by runApplicationThread().
	acquire_sem(fSemAppRun);
	
	app->Run();
	
	jvm->DetachCurrentThread();
	
	return B_OK;
}

JNIEXPORT void JNICALL
Java_java_awt_Toolkit_initIDs
  (JNIEnv * env, jclass klass)
{
}
