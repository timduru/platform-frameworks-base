package com.android.systemui.kat;

  public class ButtonSpec {
    public static final String KEY = "key";


    public static final String SIZE_MOD_START = "[";
    public static final String SIZE_MOD_END = "]";

    public static final String KEY_CODE_START = "(";
    public static final String KEY_IMAGE_DELIM = ":";
    public static final String KEY_CODE_END = ")";

    public static final String EXTRA = "|";

    public String button;
    public int keyCode;
    public String image; // if keycode
    public float size;
    public String extra;

    public ButtonSpec(String spec) {
        button = extractButton(extractExtra(spec, true));
        extra = extractExtra(spec, false);
        image = extractButton(spec);
        keyCode = extractKeyCode(spec);
        size = extractSize(spec);
    }

    public String extractImage(String buttonSpec) {
        if (!buttonSpec.contains(KEY_IMAGE_DELIM)) {
            return null;
        }
        final int start = buttonSpec.indexOf(KEY_IMAGE_DELIM);
        String subStr = buttonSpec.substring(start + 1, buttonSpec.indexOf(KEY_CODE_END));
        return subStr;
    }

    public int extractKeyCode(String buttonSpec) {
        if (!buttonSpec.contains(KEY_CODE_START)) {
            return 1;
        }
        final int start = buttonSpec.indexOf(KEY_CODE_START);
        String subStr = buttonSpec.substring(start + 1, buttonSpec.indexOf(KEY_IMAGE_DELIM));
        return Integer.parseInt(subStr);
    }

    public float extractSize(String buttonSpec) {
        if (!buttonSpec.contains(SIZE_MOD_START)) {
            return 0.5f;
        }
        final int sizeStart = buttonSpec.indexOf(SIZE_MOD_START);
        String sizeStr = buttonSpec.substring(sizeStart + 1, buttonSpec.indexOf(SIZE_MOD_END));
        return Float.parseFloat(sizeStr);
    }

    public String extractButton(String buttonSpec) {
        if (!buttonSpec.contains(SIZE_MOD_START) && !buttonSpec.contains(EXTRA)) {
            return buttonSpec;
        }
        int end = buttonSpec.indexOf(SIZE_MOD_START);
        if(end == -1) end = buttonSpec.indexOf(EXTRA);
        return buttonSpec.substring(0, end);
    }

    public String extractExtra(String buttonSpec, boolean stripExtra) {
       String[] spec_extra =  buttonSpec.split("\\" + EXTRA);
       if(stripExtra) return spec_extra[0];
       else if (spec_extra.length >= 2) return spec_extra[1];
       else return null;
    }

   public String getSpec() {
      return button
                  + SIZE_MOD_START + size + SIZE_MOD_END
                  + (extra==null?"": EXTRA + extra);
   }
  }

