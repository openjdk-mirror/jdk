#ifndef TEXT_COMPONENT_ADAPTER_H
#define TEXT_COMPONENT_ADAPTER_H

#include "ComponentAdapter.h"
#include <interface/Rect.h>

class BTextView;
class TextArea;
class TextField;

class TextComponentAdapter : public ComponentAdapter {
private:
	BTextView * textView;

public: 	// accessor
	BTextView * GetTextView() { return textView; }

protected: 	// initialization
	        TextComponentAdapter(JNIEnv * jenv, jobject jpeer, jobject jparent, TextArea * textArea);
	        TextComponentAdapter(JNIEnv * jenv, jobject jpeer, jobject jparent, TextField * textControl);

public: 	// destruction
	virtual ~TextComponentAdapter();
	
public: 	// JNI entry points
	virtual void	enable();
	virtual void	disable();
	        void	setEditable(bool editable);
	  const char *	getText();
	        void	setText(char * l);
	        int 	getSelectionStart();
	        int 	getSelectionEnd();
	        void	select(int selStart, int selEnd);
	        void	setCaretPosition(int pos);
	        int 	getCaretPosition();
	        int 	getIndexAtPoint(int x, int y);
	        BRect	getCharacterBounds(int i);
	        long	filterEvents(long mask);
};

#endif // TEXT_COMPONENT_ADAPTER_H
