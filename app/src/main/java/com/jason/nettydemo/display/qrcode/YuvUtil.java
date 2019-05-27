package com.jason.nettydemo.display.qrcode;

public class YuvUtil {

	public static void rotateNV21_270(byte[] src, byte[] dst, int w, int h) {
		// Rotate the Y luma
		int i = 0;

		for (int x = w - 1; x >= 0; x--) {

			for (int y = 0; y < h; y++) {

				dst[i] = src[y * w + x];
				i++;

			}
		} // Rotate the U and V color components i = imageWidth*imageHeight;

		for (int x = w - 1; x > 0; x = x - 2) {
			for (int y = 0; y < h / 2; y++) {
				dst[i] = src[(w * h) + (y * w) + (x - 1)];
				i++;
				dst[i] = src[(w * h) + (y * w) + x];
				i++;
			}
		}
	}

	public static void rotateNV21_90(byte[] src, byte[] dst, int w, int h) {
		// Rotate the Y luma
		int i = 0;

		for (int x = 0; x < w; x++) {

			for (int y = h - 1; y >= 0; y--) {
				dst[i] = src[y * w + x];
				i++;
			}
		}

		// Rotate the U and V color components
		i = w * h * 3 / 2 - 1;
		for (int x = w - 1; x > 0; x = x - 2) {
			for (int y = 0; y < h / 2; y++) {
				dst[i] = src[(w * h) + (y * w) + x];
				i--;
				dst[i] = src[(w * h) + (y * w) + (x - 1)];
				i--;
			}
		}
	}
	
	public static void rotateNV21_180(byte[] src, byte[] dst, int w, int h) {
		int i = 0;
		int count = 0;
		for (i = w * h - 1; i >= 0; i--) {
			dst[count] = src[i];
			count++;
		}
		i = w * h * 3 / 2 - 1;
		for (i = w * h * 3 / 2 - 1; i >= w * h; i -= 2) {
			dst[count++] = src[i - 1];
			dst[count++] = src[i];
		}
	}

	public static void rotateNV21(byte[] src, byte[] dst, int w, int h, int degree) {
		if (degree == 90) {
			rotateNV21_90(src, dst, w, h);
		}else if (degree == 180) {
			rotateNV21_180(src, dst, w, h);
		}else if (degree == 270) {
			rotateNV21_270(src, dst, w, h);
		}
	}
}
