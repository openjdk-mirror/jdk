#include "MenuAdapter.h"
#include <app/Message.h>
#include <interface/Menu.h>
#include <interface/MenuItem.h>
#include <interface/Window.h>
#include <cstdio>

static BMenuItem *
make_menu_item(BMenu * menu)
{
	return new BMenuItem(menu, NULL);
}


/* static */ BMenu *
MenuAdapter::NewMenu(JNIEnv * jenv, jobject jpeer, jobject jparent)
{
	// TOTRY: pass target as an argument
	jobject jjavaObject = jenv->GetObjectField(jpeer, javaObject_ID);
	DASSERT(jjavaObject != NULL);
	// TOTRY: pass label as an argument
	jstring jlabel = (jstring)jenv->GetObjectField(jjavaObject, label_ID);
	const char * label = (jlabel != NULL ? jenv->GetStringUTFChars(jlabel, NULL) : NULL);
	BMenu * value = new BMenu(label);
	if (label != NULL) {
		jenv->ReleaseStringUTFChars(jlabel, label);
	}
	return value;
}


MenuAdapter::MenuAdapter(JNIEnv * jenv, jobject jpeer, jobject jparent, BMenu * menu) 
	: MenuItemAdapter(jenv, jpeer, jparent, make_menu_item(menu)),
	  menu(NULL)
{
	this->menu = menu;
}


MenuAdapter::~MenuAdapter()
{
	DASSERT(menu == NULL);
}

// #pragma mark -
//
// JNI entry points
//

/* virtual */ void
MenuAdapter::dispose()
{
	// menu will be deleted when the containing BMenuItem is deleted in dispose
	menu = NULL;
	// MenuAdapter will be deleted from MenuItemAdapter::dispose
	MenuItemAdapter::dispose();
}


void
MenuAdapter::addItem(BMenuItem * item)
{
	// All items are already added to the menu during addNotify.
	// see java.awt.Menu.add(MenuItem mi);
	DASSERT(item->Menu() == this->menu);
}


void
MenuAdapter::delItem(int index)
{
	// All items are already removed from the menu during removeNotify.
	// see java.awt.Menu.remove(int index);
}


// #pragma mark -
//
// JNI
//

#include "java_awt_Menu.h"

/*
 * Class:     java_awt_Menu
 * Method:    initIDs
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_java_awt_Menu_initIDs
  (JNIEnv * jenv, jclass jklass)
{
	// nothing to do
}

