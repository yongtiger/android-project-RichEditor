package cc.brainbook.android.richeditortoolbar.span;

import android.os.Parcel;
import android.text.style.URLSpan;

import com.google.gson.annotations.Expose;

import cc.brainbook.android.richeditortoolbar.interfaces.IBlockCharacterStyle;

public class CustomURLSpan extends URLSpan implements IBlockCharacterStyle {
    ///[Gson#Exclude父类成员变量的序列化和反序列化]
    ///Exclude后父类成员变量不被序列化，因此需要重新声明并设置@Expose
    @Expose
    private final String mURL;


    /**
     * Constructs a {@link CustomURLSpan} from a url string.
     *
     * @param url the url string
     */
    public CustomURLSpan(String url) {
        super(url);
        mURL = url;
    }


    public static final Creator<CustomURLSpan> CREATOR = new Creator<CustomURLSpan>() {
        @Override
        public CustomURLSpan createFromParcel(Parcel in) {
            final String url = in.readString();

            return new CustomURLSpan(url);
        }

        @Override
        public CustomURLSpan[] newArray(int size) {
            return new CustomURLSpan[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mURL);
    }

}
