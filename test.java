
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
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Slider;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import javax.swing.*;
import java.awt.*;
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

		Image top_image = GetSlice();;
		Image nearestneibour = NearestNeigbour((int)top_image.getWidth(),(int)top_image.getHeight());


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
					Image bilinear = BilinearInterpolation(max_widthval,max_heightval);




					if (rb1.isSelected()) {
						TopView.setImage(resiedimg);

					} else if(rb2.isSelected()) {
						TopView.setImage(bilinear);


					}





				}
			});



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
		Scene scene = new Scene(root, 824, 568);
		stage.setScene(scene);
		stage.show();




		ThumbWindow(scene.getX() + 100, scene.getY() + 100);

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
				for(int i=0; i < 74; i++) {
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

	public Image NearestNeigbour(int maxw, int maxh) {

		WritableImage new_image = new WritableImage(maxw, maxh);
		//Find the width and height of the image to be process

			int height = (int)GetSlice().getHeight();
			int width = (int)GetSlice().getWidth();

			//Get an interface to write to that image memory
			PixelWriter newimage_writer = new_image.getPixelWriter();

		//Iterate over all pixels
		for (int j = 0; j < new_image.getHeight()-1; j++) {
				for (int i = 0; i  < new_image.getWidth()-1; i++) {
					for (int c = 0; c <= 2; c++) {
						//For each pixel, get the colour from the cthead slice 76

						int y = j * width / (int)maxw;
						int x = i * height / (int)maxh;

						float val2 = grey[76][y][x];
						Color color = Color.color(val2, val2, val2);
						//Apply the new colour
						newimage_writer.setColor(i, j, color);
					}
				}
			}
			return new_image;
		}

		public  Image BilinearInterpolation(int maxw,int maxh){

			WritableImage new_image2 = new WritableImage(maxw, maxh);

			int height = (int)GetSlice().getHeight();
			int width = (int)GetSlice().getWidth();


			PixelWriter newimage_writer = new_image2.getPixelWriter();


			for(int i=0; i< maxh; i++){
				for(int j=0; j < maxw; j++){

					int y_ratio = j * width / (int)maxw;
					int x_ratio = i * height / (int)maxh;

					//Source points coordinates
					double x0, y0, x1, y1 ,x2, y2;
					int x01, y01, x02, y02;

					x0 = (int) j  * x_ratio;
					y0 = (double) i * y_ratio;
					y01 = (int) y0;
					x01 = (int) x0;
					x0 = (int) x0;
					y02 = (y01 == maxh ) ? y01 : y01 + 1;
					x02 = (x01 == maxw ) ? x01 : x01 + 1;
					x2 = x0 - (double) x01;
					y2 = y0 - (double) y01;
					x01 = 1 - x01;
					y01 = 1 - y01;

					float val2 = grey[76][x_ratio][y_ratio];
					Color color = Color.color(val2, val2, val2);
					//Apply the new colour
					newimage_writer.setColor(y_ratio, x_ratio, color);

					//float val1 = grey[76][y01][x01];
					//float val2 = grey[76][y01][x02];
					//float val3 = grey[76][y02][x01];
					//float val4 = grey[76][y02][x02];

					//int col = (int)(y2 * (x2 * (val1) + x2 * (val2)) +
					//		      y01 * (x2 * (val3) + x01 * (val4)));

					//Color casda = Color.color(col,col,col);

					//newimage_writer.setColor(i,j, casda);
					//float val5 = grey[76,g,g];

					//newimage_writer.setColor();

				}
			}
		return new_image2;
		}







	public void ThumbWindow(double atX, double atY) {
		StackPane ThumbLayout = new StackPane();


		ThumbLayout.setLayoutX(50);

		WritableImage thumb_image = new WritableImage(400, 500);


		//GetRandomSlice(16)
		//This will generate an image from the slice by passing the desired grey value as a parameter.
		ImageView thumb_view = new ImageView(GetRandomSlice(8));
		thumb_view.setFitWidth(100);
		thumb_view.setFitHeight(100);

		ImageView thumb_view2 = new ImageView(GetRandomSlice(16));
		thumb_view.setFitWidth(200);
		thumb_view2.setFitHeight(200);




			//ImageView sample_thumb = new ImageView(GetRandomSlice(16));
			//sample_thumb.setLayoutX(24);
			//sample_thumb.setLayoutY(19);

			//ImageView sample_thumb2 = new ImageView(GetRandomSlice(20));
			//sample_thumb2.setLayoutX(44);
			//sample_thumb2.setLayoutY(53);

			//ThumbLayout.getChildren().add(sample_thumb2);
			ThumbLayout.getChildren().add(thumb_view2);
			ThumbLayout.getChildren().add(thumb_view);





		{

			//This bit of code makes a white image
			PixelWriter image_writer = thumb_image.getPixelWriter();


			Color color=Color.color(1,1,1);
			for(int y = 0; y < thumb_image.getHeight(); y++) {
				for(int x = 0; x < thumb_image.getWidth(); x++) {
					//Apply the new colour
					image_writer.setColor(x, y, color);



				}
			}
		}




		Scene ThumbScene = new Scene(ThumbLayout, thumb_image.getWidth(), thumb_image.getHeight());


		
		//Add mouse over handler - the large image is change to the image the mouse is over
		thumb_view.addEventHandler(javafx.scene.input.MouseEvent.MOUSE_MOVED, event -> {
			System.out.println(event.getX()+"  "+event.getY());
			event.consume();
		});
	
		//Build and display the new window
		Stage newWindow = new Stage();

		newWindow.setTitle("CThead Slices");

		newWindow.setScene(ThumbScene);


	
		// Set position of second window, related to primary window.
		newWindow.setX(atX);
		newWindow.setY(atY);
	
		newWindow.show();
	}

	
    public static void main(String[] args) {


		launch();


    }


}