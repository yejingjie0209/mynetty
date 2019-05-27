package com.jason.nettydemo.display.qrcode;

import android.graphics.Rect;
import com.google.zxing.*;
import com.google.zxing.common.HybridBinarizer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class CameraPreviewDecoder {
	private final MultiFormatReader multiFormatReader;

	public CameraPreviewDecoder() {
		multiFormatReader = new MultiFormatReader();

		Collection<BarcodeFormat> decodeFormats = new ArrayList<BarcodeFormat>();
		Map<DecodeHintType, Object> baseHints = new HashMap<DecodeHintType, Object>();
		String characterSet = "utf8";

		// The prefs can't change while the thread is running, so pick them up
		// once here.
		decodeFormats.addAll(DecodeFormatManager.QR_CODE_FORMATS);

		baseHints.put(DecodeHintType.POSSIBLE_FORMATS, decodeFormats);

		if (characterSet != null) {
			baseHints.put(DecodeHintType.CHARACTER_SET, characterSet);
		}

		multiFormatReader.setHints(baseHints);
	}
	
	public Result decode(byte[] data, int width, int height, Rect rect) {
		Result rawResult = null;
		
		PlanarYUVLuminanceSource source = new PlanarYUVLuminanceSource(data, width, height, rect.left, rect.top,
                rect.width(), rect.height(), false);
		if (source != null) {
			BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
			try {
				rawResult = multiFormatReader.decodeWithState(bitmap);
			} catch (ReaderException re) {
				// continue
			} finally {
				multiFormatReader.reset();
			}
		}
		
		return rawResult;
	}
}
