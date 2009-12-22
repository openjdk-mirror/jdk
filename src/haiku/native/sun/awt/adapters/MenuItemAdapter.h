#ifndef MENU_ITEM_ADAPTER_H
#define MENU_ITEM_ADAPTER_H

#include "MenuComponentAdapter.h"

#define AWT_MENU_ITEM_INVOKED 'aMIK'
#define AWT_MENU_ITEM_ADAPTER_POINTER "MenuItemAdapter"

class BMenu;
class BMenuItem;
class EventEnvironment;

class MenuItemAdapter : public MenuComponentAdapter {
private:
	BMenuItem * menuitem;

public: 	// accessor
	BMenuItem * MenuItem() { return menuitem; }

public: 	// initialization
	static BMenuItem * NewMenuItem(JNIEnv * jenv, jobject jpeer, jobject jparent);
	        MenuItemAdapter(JNIEnv * jenv, jobject jpeer, jobject jparent, BMenuItem * menuitem);

public: 	// destruction
	virtual ~MenuItemAdapter();
	
public: 	// JNI entry points
	virtual void	dispose();
	        void	setShortcut(char shortcut, bool shifted);
	        void	setLabel(char * label);
	        void	setEnabled(bool enabled);

public:		// ComponentAdapter invocation callback
	virtual void	Invoked(EventEnvironment * environment, BMessage * message);

public: 	// -- Field and Method ID cache
		static jfieldID label_ID;
		static jmethodID command_ID;
};

#endif // MENU_ITEM_ADAPTER_H
