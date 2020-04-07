package ir.mehdivijeh.scanner.main;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.widget.Button;

import java.util.ArrayList;
import java.util.List;

import ir.mehdivijeh.scanner.R;
import ir.mehdivijeh.scanner.wrapper.ImageWrapper;
import ir.mehdivijeh.scanner.wrapper.ImageWrapperBuilder;

public class MainActivity extends ChooseAvatarAbstract {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button btnSelectImage = findViewById(R.id.btn_open_image);

        btnSelectImage.setOnClickListener(v -> showTakeImagePopup(false));

    }

    @Override
    public void onImageProvided(Drawable drawable, String path) {
        createImageWrapper(path);
    }

    private void createImageWrapper(String mImagePath) {
        /*
        |        |                  |        |
        |    B   |                  A        C
        | /    \ |                  | \    / |
        A        C                  |   B    |
        |        |                  |        |
        |        |       OR         |        |
        |        |                  |        |
        F        D                  F        D
        | \    / |                  | \    / |
        |   E    |                  |   E    |
        |        |                  |        |
        * */
        List<double[]> pointsPercent = new ArrayList<>();
        pointsPercent.add(new double[]{0.0, 0.125});  //A
        pointsPercent.add(new double[]{0.265, 0.089});  //B
        pointsPercent.add(new double[]{0.637, 0.152});  //C
        pointsPercent.add(new double[]{0.703, 0.810});  //D
        pointsPercent.add(new double[]{0.265, 0.867});  //E
        pointsPercent.add(new double[]{0.0, 0.820});  //F

        ImageWrapper imageWrapper = new ImageWrapperBuilder(this)
                .setImagePath(mImagePath)
                .setPointsPercent(pointsPercent)
                .build();

        imageWrapper.unwrap();
    }

    @Override
    public void onDeleteImageClicked() {

    }
}
