#ifndef POPUP_MENU_ADAPTER_H
#define POPUP_MENU_ADAPTER_H

#include "MenuAdapter.h"

class BPopUpMenu;

class PopupMenuAdapter : public MenuAdapter {
protected:
	BPopUpMenu * popup;

public: 	// accessor
	BPopUpMenu * PopupMenu() { return popup; }

public: 	// initialization
	static BPopUpMenu * NewPopupMenu(JNIEnv * jenv, jobject jpeer, jobject jparent);
	        PopupMenuAdapter(JNIEnv * jenv, jobject jpeer, jobject jparent);

public: 	// destruction
	virtual ~PopupMenuAdapter();
	
public: 	// JNI entry points
	virtual void	dispose();
	        void	show(BView * view, int x, int y);
};

#endif // POPUP_MENU_ADAPTER_H
