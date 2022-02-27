
/*
CS-255 Getting started code for the assignment
I do not give you permission to post this code online
Do not post your solution online
Do not copy code
Do not use JavaFX functions or other libraries to do the main parts of the assignment:
	1. Creating a resized image (you must implement nearest neighbour and bilinear interpolation yourself
	2. Gamma correcting the image
	3. Creating the image which has all the thumbnails and event handling to change the larger image
All of those functions must be written by yourself
You may use libraries / IDE to achieve a better GUI
*/

import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Slider;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.*;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;

public class test extends Application {
	short cthead[][][]; //store the 3D volume data set
	float grey[][][]; //store the 3D volume data set converted to 0-1 ready to copy to the image
	short min, max; //min/max value in the 3D volume data set
	ImageView TopView;

	@Override
	public void start(Stage stage) throws FileNotFoundException {
		stage.setTitle("CThead Viewer");

		try {
			ReadData();
		} catch (IOException e) {
			System.out.println("Error: The CThead file is not in the working directory");
			System.out.println("Working Directory = " + System.getProperty("user.dir"));
			return;
		}


		//int width=1024, height=1024; //maximum size of the image
		//We need 3 things to see an image
		//1. We need to create the image


		//Image top_image = GetSlice(); //go get the slice image

		Image top_image = GetSlice();
		;
		//Image nearestneibour = NearestNeigbour((int)top_image.getWidth(),(int)top_image.getHeight());


		//2. We create a view of that image
		TopView = new ImageView(top_image); //and then see 3. below


		//Create the simple GUI
		final ToggleGroup group = new ToggleGroup();

		RadioButton rb1 = new RadioButton("Nearest neighbour");
		rb1.setToggleGroup(group);
		rb1.setSelected(true);


		RadioButton rb2 = new RadioButton("Bilinear");
		rb2.setToggleGroup(group);


		Slider szslider = new Slider(32, 1024, 256);

		Slider gamma_slider = new Slider(.1, 4, 1);


		//WritableImage changed_image = new WritableImage((int) szslider.getWidth(),(int) szslider.getHeight());
		//float scalefactor = (float)top_image.getWidth()/(float) changed_image.getWidth();


		//Radio button changes between nearest neighbour and bilinear
		group.selectedToggleProperty().addListener(new ChangeListener<Toggle>() {
			public void changed(ObservableValue<? extends Toggle> ob, Toggle o, Toggle n) {

				if (rb1.isSelected()) {
					System.out.println("Radio button 1 clicked");


				} else if (rb2.isSelected()) {
					System.out.println("Radio button 2 clicked");


				}
			}
		});


		//Size of main image changes (slider)

		szslider.valueProperty().addListener(new ChangeListener<Number>() {
			public void changed(ObservableValue<? extends Number>
										observable, Number oldValue, Number newValue) {

				System.out.println(newValue.intValue());
				//Here's the basic code you need to update an image
				//TopView.setImage(null); //clear the old image
				//Image newImage=GetSlice(); //go get the slice image
				//TopView.setImage(newImage); //Update the GUI so the new image is displayed


				TopView.setImage(null);
				Image newImage = GetSlice();
				int max_heightval = newValue.intValue();
				int max_widthval = newValue.intValue();

				Image resiedimg = NearestNeigbour(max_widthval, max_heightval);


				if (rb1.isSelected()) {
					TopView.setImage(resiedimg);

				} else if (rb2.isSelected()) {


				}


			}
		});

		//GammaCorrection((int)gamma_slider.getWidth(),(int)gamma_slider.getHeight(), gamma_slider.getMax());


		//Gamma value changes
		gamma_slider.valueProperty().addListener(new ChangeListener<Number>() {
			public void changed(ObservableValue<? extends Number>
										observable, Number oldValue, Number newValue) {

				System.out.println(newValue.doubleValue());
			}
		});

		VBox root = new VBox();

		//Add all the GUI elements
		//3. (referring to the 3 things we need to display an image)
		//we need to add it to the layout


		root.getChildren().addAll(rb1, rb2, gamma_slider, szslider, TopView);

		//Display to user
		Scene scene = new Scene(root, 490, 600);

		stage.setScene(scene);
		stage.setX(-5);
		stage.setY(2);
		stage.show();


		ThumbWindow(scene.getX() + 480, scene.getY() - 20);


	}

	//After Scaling the Image


	//Function to read in the cthead data set
	public void ReadData() throws IOException {
		//File name is hardcoded here - much nicer to have a dialog to select it and capture the size from the user
		File file = new File("CThead");
		//Read the data quickly via a buffer (in C++ you can just do a single fread - I couldn't find the equivalent in Java)
		DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(file)));

		int i, j, k; //loop through the 3D data set

		min = Short.MAX_VALUE;
		max = Short.MIN_VALUE; //set to extreme values
		short read; //value read in
		int b1, b2; //data is wrong Endian (check wikipedia) for Java so we need to swap the bytes around

		cthead = new short[113][256][256]; //allocate the memory - note this is fixed for this data set
		grey = new float[113][256][256];
		//loop through the data reading it in
		for (k = 0; k < 113; k++) {
			for (j = 0; j < 256; j++) {
				for (i = 0; i < 256; i++) {
					//because the Endianess is wrong, it needs to be read byte at a time and swapped
					b1 = ((int) in.readByte()) & 0xff; //the 0xff is because Java does not have unsigned types (C++ is so much easier!)
					b2 = ((int) in.readByte()) & 0xff; //the 0xff is because Java does not have unsigned types (C++ is so much easier!)
					read = (short) ((b2 << 8) | b1); //and swizzle the bytes around
					if (read < min) min = read; //update the minimum
					if (read > max) max = read; //update the maximum
					cthead[k][j][i] = read; //put the short into memory (in C++ you can replace all this code with one fread)
				}
			}
		}
		System.out.println(min + " " + max); //diagnostic - for CThead this should be -1117, 2248
		//(i.e. there are 3366 levels of grey, and now we will normalise them to 0-1 for display purposes
		//I know the min and max already, so I could have put the normalisation in the above loop, but I put it separate here
		for (k = 0; k < 113; k++) {
			for (j = 0; j < 256; j++) {
				for (i = 0; i < 256; i++) {
					grey[k][j][i] = ((float) cthead[k][j][i] - (float) min) / ((float) max - (float) min);
				}
			}
		}


	}

	//Gets an image from slice 76
	public Image GetSlice() {

		WritableImage image = new WritableImage(256, 256);
		//Find the width and height of the image to be process
		int width = (int) image.getWidth();
		int height = (int) image.getHeight();
		float val;


		//Get an interface to write to that image memory
		PixelWriter image_writer = image.getPixelWriter();


		//Iterate over all pixels
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				//For each pixel, get the colour from the cthead slice 76
				for (int i = 0; i < 74; i++) {
					val = grey[75][y][x];
					Color color = Color.color(val, val, val);


					//Apply the new colour
					image_writer.setColor(x, y, color);
				}
			}
		}
		return image;
	}

	public Image GetRandomSlice(int s) {

		WritableImage image = new WritableImage(256, 256);
		//Find the width and height of the image to be process
		int width = (int) image.getWidth();
		int height = (int) image.getHeight();
		float val;


		//Get an interface to write to that image memory
		PixelWriter image_writer = image.getPixelWriter();


		//Iterate over all pixels
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				//For each pixel, get the colour from the cthead slice 76
				val = grey[s][y][x];
				Color color = Color.color(val, val, val);


				//Apply the new colour
				image_writer.setColor(x, y, color);

			}
		}
		return image;
	}

	//This method implements Nearest Neighbour Interpolation in order to resize the image.
	//Nearest Neighbour Interpolation happens as follows:
	//-Parameters of the slider are passed down as parameters.
	//-A pixelwriter is used to draw the image.
	//-
	//
	public Image NearestNeigbour(int maxw, int maxh) {

		WritableImage new_image = new WritableImage(maxw, maxh);


		//Width and Height of the original image to be resized.
		int height = (int) GetSlice().getHeight();
		int width = (int) GetSlice().getWidth();

		//Get an interface to write to that image memory
		PixelWriter newimage_writer = new_image.getPixelWriter();

		//Iterate over all the pixxels of the Image
		for (int j = 0; j < new_image.getHeight() - 1; j++) {
			for (int i = 0; i < new_image.getWidth() - 1; i++) {
				for (int c = 0; c <= 2; c++) {
					//For each pixel, get the colour from the cthead slice 76

					//These are the width and height of the new images.
					int y = j * width / (int) maxw;
					int x = i * height / (int) maxh;

					float val2 = grey[76][y][x];
					Color color = Color.color(val2, val2, val2);
					//Apply the new colour
					newimage_writer.setColor(i, j, color);
				}
			}
		}
		return new_image;
	}

	public Image BilinearInterpolation(int maxw, int maxh) {

		WritableImage new_image2 = new WritableImage(maxw, maxh);
		//Find the width and height of the image to be process

		int old_height = (int) GetSlice().getHeight();
		int old_width = (int) GetSlice().getWidth();


		int y_ratio = old_width / (int) maxw;
		int x_ratio = old_height / (int) maxh;

		//Get an interface to write to that image memory
		PixelWriter newimage_writer = new_image2.getPixelWriter();

		//Iterate over all pixels
		for (int j = 0; j < new_image2.getHeight() - 1; j++) {
			for (int i = 0; i < new_image2.getWidth() - 1; i++) {
				for (int c = 0; c <= 2; c++) {
					//For each pixel, get the colour from the cthead slice 76

					int x1 = j * x_ratio;
					int y1 = i * y_ratio;

					float x_diff = (x_ratio * i) - x1;
					float y_diff = (y_ratio * i) - y1;
					float index = y1 * old_width + x1;


					float val1 = grey[76][y1][x1];
					float val2 = grey[76][y1][x1 + 1];
					float val3 = grey[76][y1 + 1][x1];
					float val4 = grey[76][y1 + 1][x1 + 1];

					float final_grey = (((val1) * (1 - x_diff) * (1 - y_diff)) + ((val2) * (x_diff) * (1 - y_diff)) + ((val3) * (y_diff) * (1 - x_diff)) + ((val4) * (x_diff * y_diff)));


					Color final_color = Color.color(final_grey, final_grey, final_grey);
					//Apply the new colour
					newimage_writer.setColor(i, j, final_color);
				}


			}
		}
		return new_image2;
	}


	//This method is used to Implement gamma correction of the image. Width and height of the resized(Up to date) Image is passed down as parameters.

	public Image GammaCorrection(int maxw, int maxh, double gamma) {

		double gamma_new = 1 / gamma;

		WritableImage gamma_corected = new WritableImage(maxw, maxh);


		for (int i = 0; i < GetSlice().getWidth(); i++) {
			for (int j = 0; j < GetSlice().getHeight(); j++) {
			}
		}
		return gamma_corected;
	}


	public void ThumbWindow(double atX, double atY) {

		StackPane ThumbLayout = new StackPane();

		WritableImage canvas_image = new WritableImage(780, 580);
		int canvas_height = (int)canvas_image.getHeight();
		int canvas_width = (int)canvas_image.getWidth();
		float val2;

		//The image we are fetching and it's coordinates
		Image The_slice_image = GetRandomSlice(5);


		//The image we are drawing and it's coordinates
		WritableImage new_slice_image = new WritableImage(256,256);
		int new_slice_image_height = (int)new_slice_image.getHeight();
		int new_slice_image_width = (int)new_slice_image.getWidth();

		//Draw the canvas first
		PixelWriter write_slice_image = new_slice_image.getPixelWriter();

		for(int x = 0; x < canvas_height; x++){
			for(int y =  0; y < canvas_width; y++){

				for(int i = x; i < new_slice_image_height; i++) {
					for (int j = y; j < new_slice_image_width; j++) {
							val2 = grey[76][i][j];
							Color pic_colour = Color.color(val2, val2, val2);
							write_slice_image.setColor(j,i,pic_colour);

					}
				}
			}
		}

		ImageView random_slice_image = new ImageView(new_slice_image);
		random_slice_image.setFitHeight(55);
		random_slice_image.setFitWidth(55);
		random_slice_image.setTranslateX(-500);
		random_slice_image.setTranslateY(-100);

		ThumbLayout.getChildren().add(random_slice_image);

		Scene ThumbScene = new Scene(ThumbLayout, canvas_width, canvas_height);

		Stage newWindow = new Stage();
		newWindow.setTitle("CThead Slices");
		newWindow.setScene(ThumbScene);

		newWindow.setX(atX);
		newWindow.setY(atY);
		newWindow.show();



		//Scene ThumbScene = new Scene(ThumbLayout, canvas_image.getWidth(), canvas_image.getHeight());

		//imageView.addEventHandler(javafx.scene.input.MouseEvent.MOUSE_MOVED, event -> {
		//	System.out.println(event.getX() + "  " + event.getY());
	//		event.consume();
	//	});




		//780,580


		//	for (int x = 0; x < 500; x = x + 60) {
		//		for (int y = 0; y < 500; y = y + 60) {
		//			Image new_image = GetRandomSlice(1);
		//
		//			int counter = 0;
		//			counter = counter + 1;
		//			for (int i = x; i < 50; i++) {
		//				float val = grey[counter][x][y];
		//				Color color = Color.color(val, val, val);
		//				thumbimage_writer.setColor(x, y, color);
		//			}
		//		}
		//	}


		//Add mouse over handler - the large image is change to the image the mouse is over


		//Build and display the new window


		// Set position of second window, related to primary window.


	}










		//}


    public static void main(String[] args) {


		launch();


    }


}
