#include "MenuItemAdapter.h"
#include "MenuAdapter.h"
#include "MenuBarAdapter.h"
#include "EventEnvironment.h"
#include "KeyConversions.h"
#include "Window.h"
#include <app/Message.h>
#include <interface/MenuBar.h>
#include <interface/MenuItem.h>
#include <interface/Window.h>
#include <cstdio>

/* static */ BMenuItem *
MenuItemAdapter::NewMenuItem(JNIEnv * jenv, jobject jpeer, jobject jparent)
{
	// TOTRY: pass jjavaObject as an argument
	jobject jjavaObject = jenv->GetObjectField(jpeer, javaObject_ID);
	DASSERT(jjavaObject != NULL);
	// TOTRY: pass label as an argument
	jstring jlabel = (jstring)jenv->GetObjectField(jjavaObject, label_ID);
	const char * label = (jlabel != NULL ? jenv->GetStringUTFChars(jlabel, NULL) : NULL);
	BMenuItem * value = new BMenuItem(label, new BMessage(AWT_MENU_ITEM_INVOKED));
	if (label != NULL) {
		jenv->ReleaseStringUTFChars(jlabel, label);
	}
	return value;
}


MenuItemAdapter::MenuItemAdapter(JNIEnv * jenv, jobject jpeer, jobject jparent, BMenuItem * menuitem) 
	: MenuComponentAdapter(jenv, jpeer, jparent),
	  menuitem(NULL)
{
	this->menuitem = menuitem;
	if (menuitem->Menu() == NULL) {
		DASSERT(jparent != NULL);
		ObjectAdapter * adapter = ObjectAdapter::getAdapter<ObjectAdapter>(jenv, jparent);
		MenuAdapter * menuAdapter = dynamic_cast<MenuAdapter*>(adapter);
		MenuBarAdapter * menuBarAdapter = dynamic_cast<MenuBarAdapter*>(adapter);
		if (menuAdapter != NULL) {
			menuAdapter->Menu()->AddItem(menuitem);
		} else if (menuBarAdapter != NULL) {
			menuBarAdapter->MenuBar()->AddItem(menuitem);
		}
	}
	// provide this pointer for the Invoked() callback
	BMessage * message = menuitem->Message();
	if (message != NULL) {
		// message will be NULL if this is a separator or a menuitem for a submenu
		message->AddPointer(AWT_MENU_ITEM_ADAPTER_POINTER, this);
	}
}


MenuItemAdapter::~MenuItemAdapter()
{
	DASSERT(menuitem == NULL);
}

// #pragma mark -
//
// JNI entry points
//

/* virtual */ void
MenuItemAdapter::dispose()
{
	if (menuitem != NULL) {
		BMenu * submenu = menuitem->Submenu();
		if (submenu != NULL) {
			// we should have no children by now
			DASSERT(submenu->CountItems() == 0);
		}
		BMenu * menu = menuitem->Menu();
		if (menu != NULL) {
			// disconnect from our parent
			bool result = menu->RemoveItem(menuitem);
			DASSERT(result == true);
		}
		delete menuitem; // also deletes the submenu if any (see MenuAdapter)
		menuitem = NULL;
	}
	delete this;
}


void
MenuItemAdapter::setShortcut(char shortcut, bool shifted)
{
	menuitem->SetShortcut(shortcut, (shifted ? B_SHIFT_KEY : 0));
}


void
MenuItemAdapter::setLabel(char * label)
{
	menuitem->SetLabel(label);
}


void
MenuItemAdapter::setEnabled(bool enabled)
{
	menuitem->SetEnabled(enabled);
}


// #pragma mark -
//
// ComponentAdapter invocation handoff
//


/* virtual */ void
MenuItemAdapter::Invoked(EventEnvironment * environment, BMessage * message)
{
	// parse the message
	int64 when;
	if (message->FindInt64("when", &when) != B_OK) {
		BAD_MESSAGE();
		message->PrintToStream();
		return;
	}
	jint jmodifiers = ConvertModifiersToJava(modifiers());
	// build the event
	JNIEnv * jenv = environment->env;
	jobject jevent = NULL;
	jobject jtarget = GetTarget(jenv);
	jstring jcommand = (jstring)jenv->CallObjectMethod(jtarget, command_ID);
	jevent = environment->NewActionEvent(jtarget, java_awt_event_ActionEvent_ACTION_PERFORMED,
	                                     jcommand, when, jmodifiers);
	jenv->DeleteLocalRef(jcommand);
	jenv->DeleteLocalRef(jtarget);
	jtarget = NULL;
	// send out the appropriate event
	SendEvent(jevent);
	jenv->DeleteLocalRef(jevent);
	jevent = NULL;
}


// #pragma mark -
//
// JNI
//

#include "java_awt_MenuItem.h"

jfieldID  MenuItemAdapter::label_ID = NULL;
jmethodID MenuItemAdapter::command_ID = NULL;

/*
 * Class:     java_awt_MenuItem
 * Method:    initIDs
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_java_awt_MenuItem_initIDs
  (JNIEnv * jenv, jclass jklass)
{
	MenuItemAdapter::label_ID   = jenv->GetFieldID(jklass, "label", "Ljava/lang/String;");
	MenuItemAdapter::command_ID = jenv->GetMethodID(jklass, "getActionCommand", "()Ljava/lang/String;");
	DASSERT(MenuItemAdapter::label_ID   != NULL);
	DASSERT(MenuItemAdapter::command_ID != NULL);
}

