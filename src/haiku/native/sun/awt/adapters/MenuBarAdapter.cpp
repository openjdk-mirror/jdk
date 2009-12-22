#include "MenuBarAdapter.h"
#include "FrameAdapter.h"
#include <interface/MenuBar.h>
#include <interface/MenuItem.h>
#include <interface/Window.h>
#include <cstdio>

/* static */ BMenuBar *
MenuBarAdapter::NewMenuBar(JNIEnv * jenv, jobject jpeer, jobject jparent)
{
	return new BMenuBar(BRect(0, 0, 0, 0), "MenuBar");
}


MenuBarAdapter::MenuBarAdapter(JNIEnv * jenv, jobject jpeer, jobject jparent, BMenuBar * menubar) 
	: MenuComponentAdapter(jenv, jpeer, jparent),
	  menubar(NULL)
{
	this->menubar = menubar;
	if (menubar->Window() == NULL) {
		DASSERT(jparent != NULL);
		FrameAdapter * adapter = ObjectAdapter::getAdapter<FrameAdapter>(jenv, jparent);
		adapter->setMenuBar(menubar);
	}
}


MenuBarAdapter::~MenuBarAdapter()
{
	DASSERT(menubar == NULL);
}

// #pragma mark -
//
// JNI entry points
//

/* virtual */ void
MenuBarAdapter::dispose()
{
	if (menubar != NULL) {
		// we should have no children by now
		DASSERT(menubar->CountItems() == 0);
		if (menubar->LockLooper()) {
			BWindow * window = menubar->Window();
			menubar->RemoveSelf(); // after RemoveSelf the menubar has no looper
			window->SetKeyMenuBar(NULL);
			window->UnlockLooper();
			delete menubar;
		} else if (menubar->Window() == NULL) {
			// no window means no need to lock a looper
			delete menubar;
		} else {
			LOCK_LOOPER_FAILED();
		}
		menubar = NULL;
	}
	delete this;
}


void
MenuBarAdapter::addMenu(BMenu * menu)
{
	// All menus are already added to the menubar during addNotify.
	// see java.awt.MenuBar.add(Menu m);
	DASSERT(menu->Supermenu() == this->menubar);
}


void
MenuBarAdapter::delMenu(int index)
{
	// All menus are already removed from the menubar during removeNotify.
	// see java.awt.MenuBar.remove(int index);
}


void
MenuBarAdapter::addHelpMenu(BMenu * menu)
{
	// "help" menus have no special status in haiku
	addMenu(menu);
}


// #pragma mark -
//
// JNI
//

#include "java_awt_MenuBar.h"

/*
 * Class:     java_awt_MenuBar
 * Method:    initIDs
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_java_awt_MenuBar_initIDs
  (JNIEnv * jenv, jclass jklass)
{
}

