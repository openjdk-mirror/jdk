#ifndef MENU_BAR_ADAPTER_H
#define MENU_BAR_ADAPTER_H

#include "MenuComponentAdapter.h"

class BMenu;
class BMenuBar;

class MenuBarAdapter : public MenuComponentAdapter {
protected:
	BMenuBar * menubar;

public: 	// accessor
	BMenuBar * MenuBar() { return menubar; }

public: 	// initialization
	static BMenuBar * NewMenuBar(JNIEnv * jenv, jobject jpeer, jobject jparent);
	        MenuBarAdapter(JNIEnv * jenv, jobject jpeer, jobject jparent, BMenuBar * menubar);

public: 	// destruction
	virtual ~MenuBarAdapter();
	
public: 	// JNI entry points
	virtual void	dispose();
	        void	addMenu(BMenu * menu);
	        void	delMenu(int index);
	        void	addHelpMenu(BMenu * menu);
};

#endif // MENU_BAR_ADAPTER_H
