#ifndef GLOBAL_CURSOR_ADAPTER_H
#define GLOBAL_CURSOR_ADAPTER_H

#include "jni.h"

#include <Point.h>

class ComponentAdapter;
class BCursor;

class GlobalCursorAdapter {
	public: // Singleton management, Constructor / Destructor
		static GlobalCursorAdapter* GetInstance();
		
		                            GlobalCursorAdapter();
		                            ~GlobalCursorAdapter();
	public: // JNI entry points
		void    setCursor(const BCursor * cursor, bool useCache);
		BPoint  getCursorPosition();
		jobject getComponentUnderCursor(JNIEnv *jenv);
		
	public: // Haiku subclass entry points
		void SetMouseOver(BPoint screenLocation, ComponentAdapter *adapter);
		void SetMousePosition(BPoint screenLocation);
		
	private:
		static GlobalCursorAdapter *fInstance;
		
		BPoint             fCursorLocation;
		ComponentAdapter   *fLastComponent;
		BCursor            *fLastCursor;
};

#endif /* CURSOR_ADAPTER_H */
