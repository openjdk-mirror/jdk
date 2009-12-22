#ifndef CHECKBOX_H
#define CHECKBOX_H

#include <interface/View.h>

class ComponentAdapter;
class CheckboxView;
class RadioButtonView;

class Checkbox : public BView {
private:
	ComponentAdapter * adapter;
	CheckboxView * checkbox;
	RadioButtonView * radioButton;
	bool radioMode;

public: 	// initialization & destruction
	        Checkbox(BRect frame, const char * label, bool state, bool radioMode);
	virtual ~Checkbox();
	        void SetAdapter(ComponentAdapter * adapter);
	virtual void Draw(BRect rect);
	        void SetRadioMode(bool on);
	const char * Label();
	        void SetLabel(const char * label);
	       int32 Value();
	        void SetValue(int32 value);
	        void SetEnabled(bool on);
};

#endif // CHECKBOX_H
