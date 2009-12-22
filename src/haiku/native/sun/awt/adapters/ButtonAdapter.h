#ifndef BUTTON_ADAPTER_H
#define BUTTON_ADAPTER_H

#include "ComponentAdapter.h"
#include <Point.h>

class Button;

class ButtonAdapter : public ComponentAdapter {
private:
	Button * button;

private:	// initialization
	static Button * NewButton(JNIEnv * jenv, jobject jpeer, jobject jparent);

public: 	// initialization
	        ButtonAdapter(JNIEnv * jenv, jobject jpeer, jobject jparent);

public: 	// destruction
	virtual ~ButtonAdapter();

public: 	// JNI entry points
	virtual void	enable();
	virtual void	disable();
	        void	setLabel(char * label);
	        BPoint	getMinimumSize();

protected:	// BInvoker "entry points"
	virtual void	InvocationReceived(BMessage * message);

public: 	// -- Field and Method ID cache
	static jfieldID  label_ID;
	static jmethodID command_ID;
};

#endif // BUTTON_ADAPTER_H
