# ImageConvolution plugin

Bachelor thesis project. Plugin that implements paralel image convolution for supercomputers by using OpenMPI.

Prerequisities to run are having OpenMPI and Fiji installed. 

To install plugin run Fiji and copy mpi.jar and ImageConvolution.jar into Fiji->plugins folder. After that restart Fiji and the plugin should be installed. Mpi.jar can be found it lib folder of this project and ImageConvolution.jar can be found in target folder of this project. 

It is recommended to open Fiji Console prior running the plugin. 
Running the plugin in Fiji requires opening an image first and then go to Plugins menu and select ImageConvolution. It will open a slider for user to select desired pre-configured kernel. Once kernel is selected, press ok and the plugin will start the convolution function. Once done, the result image will be saved in Fiji folder. 
