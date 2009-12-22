#include "ComponentAdapter.h"
#include "GlobalCursorAdapter.h"
#include <app/Cursor.h>
#include <app/Application.h>
#include <View.h>

GlobalCursorAdapter* GlobalCursorAdapter::fInstance = NULL;

GlobalCursorAdapter::GlobalCursorAdapter() :
		fCursorLocation(-1.0f, -1.0f)
{
	fLastComponent = NULL;
}


GlobalCursorAdapter::~GlobalCursorAdapter()
{
}


GlobalCursorAdapter* GlobalCursorAdapter::GetInstance()
{
	if (GlobalCursorAdapter::fInstance == NULL) {
		GlobalCursorAdapter::fInstance = new GlobalCursorAdapter();
	}
	return GlobalCursorAdapter::fInstance;
}


void
GlobalCursorAdapter::setCursor(const BCursor * cursor, bool useCache)
{
	if (useCache && (cursor == fLastCursor)) {
		return;
	}
	be_app->SetCursor(cursor);
}


BPoint
GlobalCursorAdapter::getCursorPosition()
{
	if (fLastComponent == NULL) {
		fCursorLocation.Set(-1.0f, -1.0f);
	}
	return fCursorLocation;
}


jobject
GlobalCursorAdapter::getComponentUnderCursor(JNIEnv *jenv)
{
	// fLastComponent might be set in another thread while we are looking
	ComponentAdapter * adapter = fLastComponent;
	// TODO: What happens if fLastComponent is disposed()? Gah!
	if (adapter != NULL) {
		return adapter->GetTarget(jenv);
	}
	return NULL;
}


void
GlobalCursorAdapter::SetMouseOver(BPoint screenLocation, ComponentAdapter *adapter)
{
	fLastComponent = adapter;
	fCursorLocation = screenLocation;
}


void GlobalCursorAdapter::SetMousePosition(BPoint screenLocation)
{
	fLastComponent = NULL;
	fCursorLocation = screenLocation;
}
