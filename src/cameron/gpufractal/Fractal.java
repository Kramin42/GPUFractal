package cameron.gpufractal;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;

import javax.imageio.ImageIO;

//import static org.lwjgl.opengl.ARBFragmentShader.*;
//import static org.lwjgl.opengl.ARBShaderObjects.*;
//import static org.lwjgl.opengl.ARBVertexShader.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL32.*;
import static org.lwjgl.opengl.GL40.*;

import org.lwjgl.BufferUtils;
import org.lwjgl.util.vector.*;



public class Fractal {
    
    private int program=0;
    
    int centerLocation;
    int invzoomLocation;
    int maxIterationLocation;
    int minIterationLocation;
    int transformParamLocation;
    int matrixLocation;
    int fracPosMatrixLocation;
    
    int extraVarsLocation;
    
    int width = 1024, height = 768;
    int texW = 1024, texH = 768;
    int saveW = 1920, saveH = 1080;
    int miniW = 400, miniH = 700;
    
    double centerx = 0.0;
    double centery = 0.0;
    double zoom = 1.0;
    double transformParam = 40.0;
    boolean autoTransformParam = false;//doesn't quite work satisfactorily
    int maxIteration = 400;
    
    float[] extraVars = {0.0f};
    
    //undo params
    double prev_centerx = -0.75f;
    double prev_centery = 0.0f;
    double prev_zoom = 0.8;
    double prev_transformParam = 40.0;
    boolean prev_autoTransformParam = true;
    int prev_maxIteration = 400;
    int prev_minIteration = 0;
    
    int renderFramebuffer;
    int renderTarget;
    int saveRenderTarget;
    int miniRenderTarget;
    
    double zoomMult = 1.2;
    boolean changed = true;

    public Fractal(String vertexShaderPath, String fragmentShaderPath, int _width, int _height, int _saveW, int _saveH, int _miniW, int _miniH, boolean antialias){
    	width = _width;
    	height = _height;
    	saveW = _saveW;
    	saveH = _saveH;
    	miniW = antialias ? 2*_miniW : _miniW;
    	miniH = antialias ? 2*_miniH : _miniH;
    	texW = antialias ? 2*width : width;
    	texH = antialias ? 2*height : height;
    	
    	int vertShader = 0, fragShader = 0;
    	
    	try {
            vertShader = createShader(vertexShaderPath,GL_VERTEX_SHADER);
            fragShader = createShader(fragmentShaderPath,GL_FRAGMENT_SHADER);
    	}
    	catch(Exception exc) {
    		exc.printStackTrace();
    		return;
    	}
    	finally {
    		if(vertShader == 0 || fragShader == 0)
    			return;
    	}
    	
    	program = glCreateProgram();
    	
    	if(program == 0)
    		return;
        
        glAttachShader(program, vertShader);
        glAttachShader(program, fragShader);
        
        glLinkProgram(program);
        if (glGetShaderi(program, GL_LINK_STATUS) == GL_FALSE) {
            System.err.println(getLogInfo(program));
            return;
        }
        
        glValidateProgram(program);
        if (glGetShaderi(program, GL_VALIDATE_STATUS) == GL_FALSE) {
        	System.err.println(getLogInfo(program));
        	return;
        }
        
        //System.out.println("using shader");
        centerLocation = glGetUniformLocation(program, "center");
        invzoomLocation = glGetUniformLocation(program, "invzoom");
        maxIterationLocation = glGetUniformLocation(program, "maxIteration");
        minIterationLocation = glGetUniformLocation(program, "minIteration");
	    transformParamLocation = glGetUniformLocation(program, "transformParam");
	    matrixLocation = glGetUniformLocation(program, "matrix");
	    fracPosMatrixLocation = glGetUniformLocation(program, "fracPosMatrix");
	    
	    extraVarsLocation = glGetUniformLocation(program, "extraVars");
	    
	    renderFramebuffer = glGenFramebuffers();
	    //saveRenderFramebuffer = glGenFramebuffers();
	    //miniRenderFramebuffer = glGenFramebuffers();
	    glBindFramebuffer(GL_FRAMEBUFFER, renderFramebuffer);
	    
	    ByteBuffer buf = null;
	    
	    renderTarget=glGenTextures();
	    saveRenderTarget=glGenTextures();
	    miniRenderTarget=glGenTextures();
	    glBindTexture( GL_TEXTURE_2D, renderTarget );
	    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
	    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
	    glTexImage2D(GL_TEXTURE_2D, 0,GL_RGBA,texW,texH,0,GL_RGBA,GL_UNSIGNED_INT, buf);
	    
	    glBindTexture( GL_TEXTURE_2D, saveRenderTarget );
	    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
	    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
	    glTexImage2D(GL_TEXTURE_2D, 0,GL_RGBA,saveW,saveH,0,GL_RGBA,GL_UNSIGNED_INT, buf);
	    
	    glBindTexture( GL_TEXTURE_2D, miniRenderTarget );
	    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
	    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
	    glTexImage2D(GL_TEXTURE_2D, 0,GL_RGBA,miniW,miniH,0,GL_RGBA,GL_UNSIGNED_INT, buf);
	    
	    glBindTexture( GL_TEXTURE_2D, renderTarget );
	    
	    glFramebufferTexture(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, renderTarget, 0);
	    
	    glDrawBuffers(GL_COLOR_ATTACHMENT0);
	    
	    if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE)
	        System.out.println("frame buffer not complete, possibly too old hardware");
    }
    
    public void prerender(boolean mini){
    	if (changed){
    		if (mini){
    			renderFractal(miniW, miniH, renderFramebuffer, miniRenderTarget);
    		} else {
    			renderFractal(texW, texH, renderFramebuffer, renderTarget);
    		}
    		changed = false;
    	}
    }
    
    public void draw(float x1, float y1, float x2, float y2, boolean mini){
        //render the texture
    	glColor4f(1.0f, 1.0f, 1.0f,1.0f);
        glEnable(GL_TEXTURE_2D);
        if (mini){
        	glBindTexture(GL_TEXTURE_2D, miniRenderTarget);
        } else {
        	glBindTexture(GL_TEXTURE_2D, renderTarget);
        }
        glBegin(GL_QUADS);
	        glTexCoord2f(0, 0); glVertex3f( x1, y1, -1.0f);
	        glTexCoord2f(1, 0); glVertex3f( x2, y1, -1.0f);
	        glTexCoord2f(1, 1); glVertex3f( x2, y2, -1.0f);
	        glTexCoord2f(0, 1); glVertex3f( x1, y2, -1.0f);
        glEnd();
        glDisable(GL_TEXTURE_2D);
    }
    
    public void zoomStep(int dir, int x, int y){
    	if (dir>0) zoom(zoomMult);
    	else if (dir<0) zoom(1/zoomMult);
    	//System.out.println("x: "+(((float)x/width-0.5)*(zoom-prevzoom))+", y: "+(((float)y/height-0.5)*(zoom-prevzoom)));
    	//move((float)(((float)x/width-0.5)*(1/zoom-1/prevzoom)),(float)(((float)y/height-0.5)*(1/zoom-1/prevzoom))); //TODO: fix this
    }
    
    public void zoom(double z){
    	zoom*=z;
    	if (autoTransformParam){
    		transformParam+= 30*Math.log(z);
    		if (transformParam < 10.0){transformParam = 10.0;}
    	}
    	//System.out.println(zoom);
    	changed = true;
    }
    
    public void move(float dx, float dy){
    	centerx+= dx;
    	centery+= dy;
        changed = true;
    }
    
    public void savePrevState(){
    	prev_centerx = centerx;
    	prev_centery = centery;
    	prev_zoom = zoom;
        prev_transformParam = transformParam;
        prev_autoTransformParam = autoTransformParam;
        prev_maxIteration = maxIteration;
    }
    
    public void loadPrevState(){
    	centerx = prev_centerx;
    	centery = prev_centery;
    	zoom = prev_zoom;
        transformParam = prev_transformParam;
        autoTransformParam = prev_autoTransformParam;
        maxIteration = prev_maxIteration;
        changed = true;
    }
    
    public void renderFractal(int width, int height, int framebuffer, int texture){
        long beforeTime = System.currentTimeMillis();
        
        // Render to our framebuffer
    	glBindFramebuffer(GL_FRAMEBUFFER, framebuffer);
        glFramebufferTexture(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, texture, 0);
        glDrawBuffers(GL_COLOR_ATTACHMENT0);
        glViewport(0,0,width,height); // Render on the whole framebuffer, complete from the lower left corner to the upper right
        
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        glUseProgram(program);
        glUniform2(centerLocation, getCenterBuffer(centerx, centery));
        glUniform1f(invzoomLocation, (float) (1.0/zoom));
        glUniform1i(maxIterationLocation, maxIteration);
        glUniform1f(transformParamLocation, (float) transformParam);
        glUniformMatrix4(matrixLocation, true, getMatrix(width,height));
        glUniformMatrix4(fracPosMatrixLocation, true, getPosMatrix(width,height));
        glUniform1(extraVarsLocation, getExtraVarsBuffer(extraVars));
        glBegin(GL_QUADS);
	        glVertex3f( 0.0f,   0.0f, -1.0f);
	        glVertex3f( width,   0.0f, -1.0f);
	        glVertex3f( width, height, -1.0f);
	        glVertex3f( 0.0f, height, -1.0f);
        glEnd();
        glFinish();
        
        
        int error = glGetError();
        if (error != GL_NO_ERROR)
            System.out.println("OpenGL Error: "+ error);
        System.out.println("render time (ms): "+(System.currentTimeMillis() - beforeTime));
    }
    
    public void saveImage(){
        
        renderFractal(saveW, saveH, renderFramebuffer, saveRenderTarget);
    	
    	glReadBuffer(GL_COLOR_ATTACHMENT0);
    	int width = saveW;
    	int height= saveH;
    	int bpp = 4; // Assuming a 32-bit display with a byte each for red, green, blue, and alpha.
    	ByteBuffer buffer = BufferUtils.createByteBuffer(width * height * bpp);
    	glReadPixels(0, 0, width, height, GL_RGBA, GL_UNSIGNED_BYTE, buffer );
    	
    	File file = new File("fractal.png"); // The file to save to.
    	int fileNum = 1;
    	while(file.exists()){
    		file = new File("fractal_"+fileNum+".png");
    		fileNum++;
    	}
    	String format = "PNG"; // Example: "PNG" or "JPG"
    	BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    	  
    	for(int x = 0; x < width; x++){
    		for(int y = 0; y < height; y++)
    		{
    			int i = (x + (width * y)) * bpp;
    			int r = buffer.get(i) & 0xFF;
    			int g = buffer.get(i + 1) & 0xFF;
    			int b = buffer.get(i + 2) & 0xFF;
    			image.setRGB(x, height - (y + 1), (0xFF << 24) | (r << 16) | (g << 8) | b);
    		}
    	}
    	  
    	try {
    		ImageIO.write(image, format, file);
    		System.out.println("saved to: "+file.getAbsolutePath());
    	} catch (IOException e) { e.printStackTrace(); }
    }
    
    private FloatBuffer getMatrix(int w, int h){
        FloatBuffer matrix = BufferUtils.createFloatBuffer(16);
    	float m1[] = {2.0f/w,0.0f,0.0f,-1.0f,0.0f, 2.0f/h,0.0f,-1.0f,0.0f,0.0f,1.0f,0.0f,0.0f,0.0f,0.0f,1.0f};
	    matrix.put(m1);
	    matrix.flip();
	    return matrix;
    }
    
    private FloatBuffer getPosMatrix(int w, int h){
        FloatBuffer fracPosMatrix = BufferUtils.createFloatBuffer(16);
	    float m2[] = {(float) w/h,0.0f,0.0f,0.0f,0.0f,1.0f,0.0f,0.0f,0.0f,0.0f,1.0f,0.0f,0.0f,0.0f,0.0f,1.0f};
	    fracPosMatrix.put(m2);
	    fracPosMatrix.flip();
	    return fracPosMatrix;
    }
    
    private DoubleBuffer getCenterBuffer(double x, double y){
    	DoubleBuffer c = BufferUtils.createDoubleBuffer(2);
    	double v[] = {x, y};
	    c.put(v);
	    c.flip();
    	return c;
    }
    
    private FloatBuffer getExtraVarsBuffer(float[] extraVars){
    	FloatBuffer e = BufferUtils.createFloatBuffer(extraVars.length);
    	e.put(extraVars);
    	e.flip();
    	return e;
    }
    
    /*
    * With the exception of syntax, setting up vertex and fragment shaders
    * is the same.
    * @param the name and path to the vertex shader
    */
    private int createShader(String filename, int shaderType) throws Exception {
    	int shader = 0;
    	try {
	        shader = glCreateShader(shaderType);
	        
	        if(shader == 0)
	        	return 0;
	        //System.out.println("-------------");
	        //System.out.println(readFileAsString(filename));
	        //System.out.println("-------------");
	        glShaderSource(shader, readFileAsString(filename));
	        glCompileShader(shader);
	        
	        if (glGetShaderi(shader, GL_COMPILE_STATUS) == GL_FALSE)
	            throw new RuntimeException("Error creating shader: " + getLogInfo(shader));
	        
	        return shader;
    	}
    	catch(Exception exc) {
    		glDeleteShader(shader);
    		throw exc;
    	}
    }
    
    private static String getLogInfo(int obj) {
        return glGetShaderInfoLog(obj, glGetShaderi(obj, GL_INFO_LOG_LENGTH));
    }
    
    private String readFileAsString(String filename) throws Exception {
        StringBuilder source = new StringBuilder();
        
        FileInputStream in = new FileInputStream(filename);
        
        Exception exception = null;
        
        BufferedReader reader;
        try{
            reader = new BufferedReader(new InputStreamReader(in,"UTF-8"));
            
            Exception innerExc= null;
            try {
            	String line;
                while((line = reader.readLine()) != null)
                    source.append(line).append('\n');
            }
            catch(Exception exc) {
            	exception = exc;
            }
            finally {
            	try {
            		reader.close();
            	}
            	catch(Exception exc) {
            		if(innerExc == null)
            			innerExc = exc;
            		else
            			exc.printStackTrace();
            	}
            }
            
            if(innerExc != null)
            	throw innerExc;
        }
        catch(Exception exc) {
        	exception = exc;
        }
        finally {
        	try {
        		in.close();
        	}
        	catch(Exception exc) {
        		if(exception == null)
        			exception = exc;
        		else
					exc.printStackTrace();
        	}
        	
        	if(exception != null)
        		throw exception;
        }
        
        return source.toString();
    }
}