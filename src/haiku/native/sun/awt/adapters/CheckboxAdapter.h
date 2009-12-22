#ifndef CHECKBOX_ADAPTER_H
#define CHECKBOX_ADAPTER_H

#include "ComponentAdapter.h"
#include <Point.h>

class Checkbox;

class CheckboxAdapter : public ComponentAdapter {
private:
	Checkbox * checkbox;
	jobject jgroup;

private:	// initialization
	static Checkbox * NewCheckbox(JNIEnv * jenv, jobject jpeer, jobject jparent);

public: 	// initialization
	        CheckboxAdapter(JNIEnv * jenv, jobject jpeer, jobject jparent);

public: 	// destruction
	virtual ~CheckboxAdapter();

private:	// utility functions
	        void	UpdateGroup(JNIEnv * jenv);

public: 	// JNI entry points
	virtual void	enable();
	virtual void	disable();
	        void	setState(bool state);
	        void	setCheckboxGroup(JNIEnv * jenv, jobject jg);
	        void	setLabel(const char * label);
	        BPoint	getMinimumSize();

protected:	// BInvoker "entry points"
	virtual void	InvocationReceived(BMessage * message);

public: 	// -- Field and Method ID cache
	static jfieldID label_ID;
	static jfieldID state_ID;
	static jfieldID group_ID;
	static jclass   checkboxGroup_ID;
	static jfieldID selectedCheckbox_ID;
};

#endif // CHECKBOX_ADAPTER_H
