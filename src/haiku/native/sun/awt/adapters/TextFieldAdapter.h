#ifndef TEXT_FIELD_ADAPTER_H
#define TEXT_FIELD_ADAPTER_H

#include "TextComponentAdapter.h"
#include <interface/Point.h>

class TextField;

class TextFieldAdapter : public TextComponentAdapter {
private:
	TextField * textField;

public:	// initialization
	static TextField * NewTextField(JNIEnv * jenv, jobject jpeer, jobject jparent);

public: 	// initialization
	        TextFieldAdapter(JNIEnv * jenv, jobject jpeer, jobject jparent);

public: 	// destruction
	virtual ~TextFieldAdapter();

public: 	// JNI entry points
	virtual void	enable();
	virtual void	disable();
	virtual BPoint	getPreferredSize();
	virtual BPoint	getMinimumSize();
	        void	setEchoChar(char echoChar);
	        BPoint	getPreferredSize(int columns);
	        BPoint	getMinimumSize(int columns);

protected:	// BInvoker "entry points"
	virtual void	InvocationReceived(BMessage * message);

};

#endif // TEXT_FIELD_ADAPTER_H
