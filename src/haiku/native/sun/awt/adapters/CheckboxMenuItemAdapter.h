#ifndef CHECKBOX_MENU_ITEM_ADAPTER_H
#define CHECKBOX_MENU_ITEM_ADAPTER_H

#include "MenuItemAdapter.h"

class BMenuItem;

class CheckboxMenuItemAdapter : public MenuItemAdapter {
protected:
	static BMenuItem * NewCheckboxMenuItem(JNIEnv * jenv, jobject jpeer, jobject jparent);

public: 	// initialization
	        CheckboxMenuItemAdapter(JNIEnv * jenv, jobject jpeer, jobject jparent);

public: 	// destruction
	virtual ~CheckboxMenuItemAdapter();
	
public: 	// JNI entry points
	        void	setState(bool enabled);

public:		// ComponentAdapter invocation callback
	virtual void	Invoked(EventEnvironment * environment, BMessage * message);

public: 	// -- Field and Method ID cache
		static jfieldID state_ID;
};

#endif // CHECKBOX_MENU_ITEM_ADAPTER_H
