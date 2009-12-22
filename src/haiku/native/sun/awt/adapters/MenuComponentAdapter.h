#ifndef MENU_COMPONENT_ADAPTER_H
#define MENU_COMPONENT_ADAPTER_H

#include "ObjectAdapter.h"

class MenuComponentAdapter : public ObjectAdapter {
protected:
	jobject jparent;

protected: 	// initialization
	MenuComponentAdapter(JNIEnv * jenv, jobject jpeer, jobject jparent);

public: 	// destruction
	virtual ~MenuComponentAdapter();
	
public: 	// JNI entry points
	virtual void	dispose() = 0;

public: 	// -- Field and Method ID cache
	static jfieldID	privateKey_ID;
};

#endif // MENU_COMPONENT_ADAPTER_H
