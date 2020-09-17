package cc.brainbook.android.richeditor;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

public class HtmlEditorActivity extends AppCompatActivity {
    private EditText mEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_html_editor);

        mEditText = findViewById(R.id.et_edit_text);


        /* -------------- ///[startActivityForResult#Activity获取数据] -------------- */
        final Intent intent = getIntent();
        final String htmlTextString = intent.getStringExtra("html_text");
        if (htmlTextString != null) {
            mEditText.setText(htmlTextString);
        }

    }


    public void btnClickSave(View view) {
        ///[startActivityForResult#setResult()返回数据]
        final Intent intent = new Intent();

        intent.putExtra("html_result", mEditText.getText().toString());

        setResult(RESULT_OK, intent);

        finish();
    }

}