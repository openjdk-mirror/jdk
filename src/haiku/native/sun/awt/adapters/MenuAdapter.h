#ifndef MENU_ADAPTER_H
#define MENU_ADAPTER_H

#include "MenuItemAdapter.h"

class BMenu;

class MenuAdapter : public MenuItemAdapter {
protected:
	BMenu * menu;

public: 	// accessor
	BMenu * Menu() { return menu; }

public: 	// initialization
	static BMenu * NewMenu(JNIEnv * jenv, jobject jpeer, jobject jparent);
	        MenuAdapter(JNIEnv * jenv, jobject jpeer, jobject jparent, BMenu * menu);

public: 	// destruction
	virtual ~MenuAdapter();
	
public: 	// JNI entry points
	virtual void	dispose();
	        void	addItem(BMenuItem * item);
	        void	delItem(int index);
};

#endif // MENU_ADAPTER_H
