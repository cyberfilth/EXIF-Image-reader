/*
 * Copyright 2002-2019 Drew Noakes and contributors
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 * More information about this project is available at:
 *
 *    https://drewnoakes.com/code/exif/
 *    https://github.com/drewnoakes/metadata-extractor
 */
package com.drew.metadata;

import java.awt.BorderLayout;
import java.awt.Container;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Arrays;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.imaging.jpeg.JpegMetadataReader;
import com.drew.imaging.jpeg.JpegProcessingException;
import com.drew.imaging.jpeg.JpegSegmentMetadataReader;
import com.drew.metadata.exif.ExifReader;
import com.drew.metadata.iptc.IptcReader;

public class ImageReader extends OutputStream {
	private final JTextArea destination;

	public ImageReader(JTextArea destination) {
		if (destination == null)
			throw new IllegalArgumentException("Destination is null");

		this.destination = destination;
	}

	public static void main(String[] args) {
		JTextArea textArea = new JTextArea(25, 80);

		textArea.setEditable(false);

		JFrame frame = new JFrame("stdout");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		Container contentPane = frame.getContentPane();
		contentPane.setLayout(new BorderLayout());
		contentPane.add(new JScrollPane(textArea, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED), BorderLayout.CENTER);
		frame.pack();
		frame.setVisible(true);

		ImageReader out = new ImageReader(textArea);
		System.setOut(new PrintStream(out));

		JFileChooser filedlg = new JFileChooser();
		filedlg.showOpenDialog(null);
		File selected = filedlg.getSelectedFile();

		String fullName = selected.getAbsolutePath();

		File file = new File(fullName);

		// There are multiple ways to get a Metadata object for a file

		//
		// SCENARIO 1: UNKNOWN FILE TYPE
		//
		// This is the most generic approach. It will transparently determine the file
		// type and invoke the appropriate
		// readers. In most cases, this is the most appropriate usage. This will handle
		// JPEG, TIFF, GIF, BMP and RAW
		// (CRW/CR2/NEF/RW2/ORF) files and extract whatever metadata is available and
		// understood.
		//
		try {
			Metadata metadata = ImageMetadataReader.readMetadata(file);

			print(metadata, "Using ImageMetadataReader");
		} catch (ImageProcessingException e) {
			print(e);
		} catch (IOException e) {
			print(e);
		}

		//
		// SCENARIO 2: SPECIFIC FILE TYPE
		//
		// If you know the file to be a JPEG, you may invoke the JpegMetadataReader,
		// rather than the generic reader
		// used in approach 1. Similarly, if you knew the file to be a TIFF/RAW image
		// you might use TiffMetadataReader,
		// PngMetadataReader for PNG files, BmpMetadataReader for BMP files, or
		// GifMetadataReader for GIF files.
		//
		// Using the specific reader offers a very, very slight performance improvement.
		//
		try {
			Metadata metadata = JpegMetadataReader.readMetadata(file);

			print(metadata, "Using JpegMetadataReader");
		} catch (JpegProcessingException e) {
			print(e);
		} catch (IOException e) {
			print(e);
		}

		//
		// APPROACH 3: SPECIFIC METADATA TYPE
		//
		// If you only wish to read a subset of the supported metadata types, you can do
		// this by
		// passing the set of readers to use.
		//
		// This currently only applies to JPEG file processing.
		//
		try {
			// We are only interested in handling
			Iterable<JpegSegmentMetadataReader> readers = Arrays.asList(new ExifReader(), new IptcReader());

			Metadata metadata = JpegMetadataReader.readMetadata(file, readers);

			print(metadata, "Using JpegMetadataReader for Exif and IPTC only");
		} catch (JpegProcessingException e) {
			print(e);
		} catch (IOException e) {
			print(e);
		}
	}

	/**
	 * Write all extracted values to stdout.
	 */
	private static void print(Metadata metadata, String method) {
		System.out.println();
		System.out.println("-------------------------------------------------");
		System.out.print(' ');
		System.out.println(method);
		System.out.println("-------------------------------------------------");
		System.out.println();

		//
		// A Metadata object contains multiple Directory objects
		//
		for (Directory directory : metadata.getDirectories()) {

			//
			// Each Directory stores values in Tag objects
			//
			for (Tag tag : directory.getTags()) {
				System.out.println(tag);
			}

			//
			// Each Directory may also contain error messages
			//
			for (String error : directory.getErrors()) {
				System.err.println("ERROR: " + error);
			}
		}
	}

	private static void print(Exception exception) {
		System.err.println("EXCEPTION: " + exception);
	}

	@Override
	public void write(int b) throws IOException {
		write(new byte[] { (byte) b }, 0, 1);
	}

	@Override
	public void write(byte[] buffer, int offset, int length) throws IOException {
		final String text = new String(buffer, offset, length);
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				destination.append(text);
			}
		});
	}
}