#ifndef TOOLKIT_ADAPTER_H
#define TOOLKIT_ADAPTER_H

#include <Application.h>
#include <OS.h>
#include <Point.h>

#include "jni.h"
#include "sun_awt_haiku_BToolkit.h"
#include "java_awt_Font.h"

class ToolkitAdapter {
	public:
		          ToolkitAdapter();
		          ~ToolkitAdapter();
		
		static    ToolkitAdapter* GetInstance();
		
		inline bool VerifyActive() { return be_app_messenger.IsValid(); }
	public: // JNI Invoked methods
		void      printSystemVersion();

		void      createApplicationThread();
		void      setPeer(JNIEnv *jenv, jobject jpeer);
		void      runApplicationThread();
		void      haltApplication();
		
		jint      getScreenResolution();
		jint      getScreenWidth();
		jint      getScreenHeight();
		
		jint      getFontSize(jint type);
		jint      getFontStyle(jint type);
		
		void      sync();
		
		jint      getMenuShortcutKeyMask();
		jboolean  isKeyLocked(jint key);
		void      setKeyLocked(jint key, jboolean on);
		
		jint      getMulticlickTime();
		
	protected:
		static    ToolkitAdapter *fInstance;
		
	private:
		bool          fEmbedded;
		jobject       fPeer;
		
		thread_id     fThidApp;
		static sem_id fSemAppRun;
		
		static int32  java_app_main_thread(void *arg);
		
		inline static bool key_down(uint8 states[16], uint32 keyCode) {
			return (states[keyCode>>3] & (1 << (7 - (keyCode%8))));
		};
	public: // -- Field and MethodID cache
		static jmethodID	getDefaultToolkit_ID;
		static jmethodID	getFontMetrics_ID;
		static jmethodID	insets_ID;
		
		/* sun.awt.haiku.BToolkit ids */
		static jmethodID	windowSettingChange_ID;
		static jmethodID	displayChange_ID;
};
#endif // TOOLKIT_ADAPTER_H
