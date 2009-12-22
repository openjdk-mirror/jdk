#include "TextComponentAdapter.h"
#include "TextArea.h"
#include "TextField.h"
#include "TextView.h"
#include <cstdio>

TextComponentAdapter::TextComponentAdapter(JNIEnv * jenv, jobject jpeer, jobject jparent, TextArea * textArea) 
	: ComponentAdapter(jenv, jpeer, jparent, textArea),
	  textView(NULL)
{
	textView = textArea->GetTextView();
	DASSERT(textView != NULL);
}


TextComponentAdapter::TextComponentAdapter(JNIEnv * jenv, jobject jpeer, jobject jparent, TextField * textField) 
	: ComponentAdapter(jenv, jpeer, jparent, textField),
	  textView(NULL)
{
	textView = textField->GetTextView();
	DASSERT(textView != NULL);
}


TextComponentAdapter::~TextComponentAdapter()
{
	// TODO: delete the TextView if necessary?
}


// #pragma mark -
//
// JNI entry points
//

void
TextComponentAdapter::enable()
{
	PRINT_PRETTY_FUNCTION();
	// TODO: anything?
}


void
TextComponentAdapter::disable()
{
	PRINT_PRETTY_FUNCTION();
	// TODO: anything?
}


void
TextComponentAdapter::setEditable(bool editable)
{
	PRINT_PRETTY_FUNCTION();
	textView->MakeEditable(editable);
}


const char *
TextComponentAdapter::getText()
{
	return textView->Text();
}


void
TextComponentAdapter::setText(char * l)
{
	if (textView->LockLooper()) {
		textView->SetText(l);
		textView->UnlockLooper();
	} else {
		LOCK_LOOPER_FAILED();
	}
}


int
TextComponentAdapter::getSelectionStart()
{
	int32 start, end;
	textView->GetSelection(&start, &end);
	return start;
}


int
TextComponentAdapter::getSelectionEnd()
{
	int32 start, end;
	textView->GetSelection(&start, &end);
	return end;
}


void
TextComponentAdapter::select(int selStart, int selEnd)
{
	textView->Select(selStart, selEnd);
}


void
TextComponentAdapter::setCaretPosition(int pos)
{
	PRINT_PRETTY_FUNCTION();
	textView->Select(pos, pos);
}


int
TextComponentAdapter::getCaretPosition()
{
	int32 start, end;
	textView->GetSelection(&start, &end);
	if (start != end) {
		fprintf(stdout, "%s: don't know caret position\n", __PRETTY_FUNCTION__);
	}
	return end;
}


int
TextComponentAdapter::getIndexAtPoint(int x, int y)
{
	BPoint point(x, y);
	return textView->OffsetAt(point);
}


// thanks Haiku
#define utf8_char_len(c) ((((int32)0xE5000000 >> ((c >> 3) & 0x1E)) & 3) + 1)


BRect
TextComponentAdapter::getCharacterBounds(int i)
{
	float height;
	BPoint topLeft = textView->PointAt(i, &height);
	const char * c = &(textView->Text()[i]);
	int size = utf8_char_len(*c);
	float width = textView->StringWidth(c, size);
	BPoint bottomRight = topLeft + BPoint(width, height);
	return BRect(topLeft, bottomRight);
}


long
TextComponentAdapter::filterEvents(long mask)
{
	PRINT_PRETTY_FUNCTION();
	DEBUGGER();
	// TODO: anything?
	return 0;
}


