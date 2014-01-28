package cameron.gpufractal;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.glUseProgram;
import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER;
import static org.lwjgl.opengl.GL30.glBindFramebuffer;

import java.awt.Font;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Random;

import org.lwjgl.BufferUtils;
import org.lwjgl.Sys;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.util.glu.GLU;
import org.newdawn.slick.Color;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.UnicodeFont;
import org.newdawn.slick.font.effects.ColorEffect;


public class Main{
	
    int sw = 400;
    int w=1280+sw;
    int h=720;
    
    boolean mouseDragging = false;
    int mdownx = 0, mdowny = 0;
    
    
	long lastFrame;
	int fps;
	long lastFPS;

    private boolean done=false;
    
    UnicodeFont font;
    
    Fractal mandelbrot;
    Fractal julia;
    boolean viewingMandelbrot = true;
    
    int palette;
    int palW = sw, palH = 300;
    
    ArrayList<float[]> palR = new ArrayList<float[]>();
    ArrayList<float[]> palG = new ArrayList<float[]>();
    ArrayList<float[]> palB = new ArrayList<float[]>();
    ArrayList<ArrayList<float[]>> pals = new ArrayList<ArrayList<float[]>>();
    boolean paletteDragging = false;
    int paletteChannel = 1;
    int palettePoint = 1;
    
    
    Random rand = new Random();
    //julia animation test vars
    //float vx = 0.0f, vy = 0.0f;
    //float ax = 0.0f, ay = 0.0f;
    
    public Main(){
        init();
        mandelbrot = new Fractal("shaders/mandelbrot.vert", "shaders/mandelbrot.frag", w-sw,h,4*1920,4*1080,sw,(int) (sw*((float) h)/(w-sw)),true);//A4@600dpi: 7020,4980
        mandelbrot.transformParam  = 30.0;
        mandelbrot.centerx = -0.75;
        mandelbrot.zoom = 0.8;
        mandelbrot.savePrevState();
        julia = new Fractal("shaders/julia.vert", "shaders/julia.frag", w-sw,h,4*1920,4*1080,sw,(int) (sw*((float) h)/(w-sw)),true);//A4@600dpi: 7020,4980
        julia.extraVars = new float[2];
        julia.extraVars[0] = -0.77f;
        julia.extraVars[1] = 0.2f;
        julia.savePrevState();
        
        ByteBuffer buf = null;
	    
	    palette=glGenTextures();
	    glBindTexture( GL_TEXTURE_2D, palette );
	    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
	    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
	    glTexImage2D(GL_TEXTURE_2D, 0,GL_RGBA,palW,palH,0,GL_RGBA,GL_UNSIGNED_INT, buf);
	    
	    pals.add(palR);
	    pals.add(palG);
	    pals.add(palB);
	    palR.add(new float[]{0.0f,0.0f});
	    palR.add(new float[]{0.4f,0.0f});
	    palR.add(new float[]{0.5f,1.0f});
	    palR.add(new float[]{1.0f,0.0f});
	    palG.add(new float[]{0.0f,0.0f});
	    palG.add(new float[]{0.3f,0.0f});
	    palG.add(new float[]{0.5f,1.0f});
	    palG.add(new float[]{0.7f,0.0f});
	    palG.add(new float[]{1.0f,0.0f});
	    palB.add(new float[]{0.0f,0.0f});
	    palB.add(new float[]{0.5f,1.0f});
	    palB.add(new float[]{0.6f,0.0f});
	    palB.add(new float[]{1.0f,0.0f});
	    
	    calcPalette();
        
        while(!done){
            if(Display.isCloseRequested())
            	done=true;
            checkInput();
            render();
            Display.update();
            Display.sync(60);
            updateFPS();
        }

        Display.destroy();
    }

    private void render(){
    	Fractal fractal = mandelbrot;
    	Fractal small_fractal = julia;
    	if (!viewingMandelbrot){
    		fractal = julia;
    		small_fractal = mandelbrot;
    	}
    	
    	//julia animation test
//    	ax += 0.0001*(rand.nextFloat() - 0.5);
//    	ay += 0.0001*(rand.nextFloat() - 0.5);
//    	ax*=0.995;
//    	ay*=0.995;
//    	vx += ax - 0.0001*(julia.extraVars[0]+0.75);
//    	vy += ay - 0.0001*(julia.extraVars[1]);
//    	float speed = (float) Math.sqrt(vx*vx+vy*vy);
//    	vx*=0.001/speed;
//    	vy*=0.001/speed;
//    	julia.extraVars[0]+= vx;
//    	julia.extraVars[1]+= vy;
//    	julia.changed = true;
    	
    	//Fractal fractal = julia;
    	fractal.prerender(false);
    	small_fractal.prerender(true);
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        glViewport(0,0,w,h);
        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();
        int size = Math.max(w, h);
        float aspect = (float)w/h;
        if (w <= h){
        	aspect = (float)w/h;
            glOrtho(0.0, size, 0.0, size*aspect, -100000.0, 100000.0);
        } else {
            glOrtho(0.0, size*aspect, 0.0, size, -100000.0, 100000.0);
        }
        glScaled(aspect, aspect, 1.0);
        glMatrixMode(GL_MODELVIEW);
        glBlendFunc (GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glEnable(GL_BLEND);
        glLoadIdentity();
        
        //draw the fractals
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        glUseProgram(0);
        fractal.draw(sw,0.0f,w,h,false);
        small_fractal.draw(0.0f,0.0f,sw,sw*((float) h)/(w-sw),true);
        
        //draw the palette (at low alpha)
        glColor4f(1.0f, 1.0f, 1.0f,0.7f);
        glEnable(GL_TEXTURE_2D);
        glBindTexture(GL_TEXTURE_2D, palette);
        glBegin(GL_QUADS);
	        glTexCoord2f(0, 1); glVertex3f( 0, sw*((float) h)/(w-sw), -1.0f);
	        glTexCoord2f(1, 1); glVertex3f( palW, sw*((float) h)/(w-sw), -1.0f);
	        glTexCoord2f(1, 0); glVertex3f( palW, sw*((float) h)/(w-sw)+palH, -1.0f);
	        glTexCoord2f(0, 0); glVertex3f( 0, sw*((float) h)/(w-sw)+palH, -1.0f);
        glEnd();
        glDisable(GL_TEXTURE_2D);
        
        //draw the r,g,b channels
        glColor4f(0.5f,0.5f,0.5f,0.5f);
        glBegin(GL_LINE_LOOP);
        	glVertex3f( 0, sw*((float) h)/(w-sw), -1.0f);
        	glVertex3f( sw, sw*((float) h)/(w-sw), -1.0f);
        	glVertex3f( sw, sw*((float) h)/(w-sw)+palH, -1.0f);
        	glVertex3f( 0, sw*((float) h)/(w-sw)+palH, -1.0f);
        glEnd();
        glBegin(GL_LINES);
        	glVertex3f( 0, sw*((float) h)/(w-sw)+palH/3, -1.0f);
        	glVertex3f( sw, sw*((float) h)/(w-sw)+palH/3, -1.0f);
        	glVertex3f( 0, sw*((float) h)/(w-sw)+2*palH/3, -1.0f);
        	glVertex3f( sw, sw*((float) h)/(w-sw)+2*palH/3, -1.0f);
        glEnd();
        //red
        float offsetY = sw*((float) h)/(w-sw)+2*palH/3;
        float multX = palW;
        float multY = palH/3.0f;
        glColor4f(1.0f,0.0f,0.0f,1.0f);
        glBegin(GL_LINE_STRIP);
        for (int i=0; i<palR.size(); i++){
        	glVertex3f(palR.get(i)[0]*multX,palR.get(i)[1]*multY+offsetY,-1.0f);
        }
        glEnd();
        //green
        offsetY = sw*((float) h)/(w-sw)+palH/3;
        glColor4f(0.0f,1.0f,0.0f,1.0f);
        glBegin(GL_LINE_STRIP);
        for (int i=0; i<palG.size(); i++){
        	glVertex3f(palG.get(i)[0]*multX,palG.get(i)[1]*multY+offsetY,-1.0f);
        }
        glEnd();
        //blue
        offsetY = sw*((float) h)/(w-sw);
        glColor4f(0.0f,0.0f,1.0f,1.0f);
        glBegin(GL_LINE_STRIP);
        for (int i=0; i<palB.size(); i++){
        	glVertex3f(palB.get(i)[0]*multX,palB.get(i)[1]*multY+offsetY,-1.0f);
        }
        glEnd();
        
        if (paletteChannel>=0 && palettePoint >= 0){
        	glEnable( GL_POINT_SMOOTH );
        	glPointSize(4);
        	offsetY = sw*((float) h)/(w-sw)+(2-paletteChannel)*palH/3;
        	float x = 0,y = 0;
        	switch (paletteChannel){
        	case 0:
        		glColor4f(1.0f,0.0f,0.0f,1.0f);
        		x = palR.get(palettePoint)[0];
        		y = palR.get(palettePoint)[1];
        		break;
        	case 1:
        		glColor4f(0.0f,1.0f,0.0f,1.0f);
        		x = palG.get(palettePoint)[0];
        		y = palG.get(palettePoint)[1];
        		break;
        	case 2:
        		glColor4f(0.0f,0.0f,1.0f,1.0f);
        		x = palB.get(palettePoint)[0];
        		y = palB.get(palettePoint)[1];
        		break;
        	}
        	glBegin(GL_POINTS);
        	glVertex3f(x*multX,y*multY+offsetY,-1.0f);
        	glEnd();
        	if (paletteDragging){
        		x = pals.get(paletteChannel).get(palettePoint)[0]*(palW);
        		y = pals.get(paletteChannel).get(palettePoint)[1]*(palH/3.0f) + sw*((float) h)/(w-sw) + (2-paletteChannel)/3.0f*palH;
        		glBegin(GL_LINES);
	        		glVertex3f(x,sw*((float) h)/(w-sw),-1.0f);
	        		glVertex3f(x,sw*((float) h)/(w-sw)+palH,-1.0f);
	        		glVertex3f(0,y,-1.0f);
	        		glVertex3f(palW,y,-1.0f);
        		glEnd();
        	}
        }
        
        
        //draw cross-hair for the Julia fractal's complex parameter on the Mandelbrot
        if (viewingMandelbrot){
        	float juliaPositionX = sw/2 + w/2 + h*0.5f*(float)mandelbrot.zoom*(julia.extraVars[0]-(float)mandelbrot.centerx);
            float juliaPositionY = h/2 + h*0.5f*(float)mandelbrot.zoom*(julia.extraVars[1]-(float)mandelbrot.centery);
        	if (juliaPositionX>sw && juliaPositionX<w && juliaPositionY>0 && juliaPositionY<h){
		        glLineWidth(1);
		    	glColor4f(1.0f,0.0f,0.0f,0.6f);
		    	glBegin(GL_LINES);
				    glVertex3f( juliaPositionX, 0, -1.0f);
				    glVertex3f( juliaPositionX, h, -1.0f);
				    glVertex3f( sw, juliaPositionY, -1.0f);
				    glVertex3f( sw+w, juliaPositionY, -1.0f);
			    glEnd();
        	}
        } else if (!viewingMandelbrot){
        	float juliaPositionX = sw/2 + sw*((float) h)/(w-sw)*0.5f*(float)mandelbrot.zoom*(julia.extraVars[0]-(float)mandelbrot.centerx);
            float juliaPositionY = (int) (0.5f*sw*((float) h)/(w-sw)) + sw*((float) h)/(w-sw)*0.5f*(float)mandelbrot.zoom*(julia.extraVars[1]-(float)mandelbrot.centery);
            if (juliaPositionX>0 && juliaPositionX<sw && juliaPositionY>0 && juliaPositionY<sw*((float) h)/(w-sw)){
		        glLineWidth(1);
		    	glColor4f(1.0f,0.0f,0.0f,0.6f);
		    	glBegin(GL_LINES);
				    glVertex3f( juliaPositionX, 0, -1.0f);
				    glVertex3f( juliaPositionX, sw*((float) h)/(w-sw), -1.0f);
				    glVertex3f( 0, juliaPositionY, -1.0f);
				    glVertex3f( sw, juliaPositionY, -1.0f);
			    glEnd();
        	}
        }
        
        // draw the mouse cross-hair
        if (Mouse.getX()>sw){
	    	glLineWidth(1);
	    	glColor4f(1.0f,1.0f,1.0f,0.6f);
	    	glBegin(GL_LINES);
			    glVertex3f( Mouse.getX(), 0, -1.0f);
			    glVertex3f( Mouse.getX(), h, -1.0f);
			    glVertex3f( sw, Mouse.getY(), -1.0f);
			    glVertex3f( sw+w, Mouse.getY(), -1.0f);
		    glEnd();
        }
        
        //draw the drag box
        if (mouseDragging){
        	glLineWidth(2);
	        glColor4f(1.0f,1.0f,1.0f,0.2f);
		    glBegin(GL_QUADS);
			    glVertex3f( mdownx, mdowny, -1.0f);
			    glVertex3f( Math.max(Mouse.getX(),sw), mdowny, -1.0f);
			    glVertex3f( Math.max(Mouse.getX(),sw),  Mouse.getY(), -1.0f);
			    glVertex3f( mdownx,  Mouse.getY(), -1.0f);
		    glEnd();
//		    glColor4f(1.0f,1.0f,1.0f,0.4f);
//		    glBegin(GL_LINE_LOOP);
//			    glVertex3f( mdownx, mdowny, -1.0f);
//			    glVertex3f(  Math.max(Mouse.getX(),sw), mdowny, -1.0f);
//			    glVertex3f(  Math.max(Mouse.getX(),sw),  Mouse.getY(), -1.0f);
//			    glVertex3f( mdownx,  Mouse.getY(), -1.0f);
//		    glEnd();
        }
        
        //draw text
        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();
        glOrtho(0, w, h, 0, 1, -1);
        glMatrixMode(GL_MODELVIEW);
        glLoadIdentity();
        glEnable(GL_BLEND);
        glDisable(GL_DEPTH_TEST);
        font.drawString(10, 10, "Zoom: "+fractal.zoom,Color.white);
        font.drawString(10, 30, "Max Iteration (Q/A): "+fractal.maxIteration,Color.white);
        font.drawString(10, 50, "Transform Param (W/S): "+fractal.transformParam,Color.white);
        glDisable(GL_TEXTURE_2D);
        
        glFinish();
    }
    
    private void checkInput(){
    	Fractal fractal = mandelbrot;
    	Fractal small_fractal = julia;
    	if (!viewingMandelbrot){
    		fractal = julia;
    		small_fractal = mandelbrot;
    	}
    	int mdx = Mouse.getDX();
    	int mdy = Mouse.getDY();
    	double modifier = 1.0;
		if (Keyboard.isKeyDown(Keyboard.KEY_LCONTROL)) modifier*=0.1;
		if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)) modifier*=10.0;
    	//System.out.println(Mouse.getX()+","+Mouse.getY());
    	if (!paletteDragging){
	    	if (Mouse.getX()>0 && Mouse.getX()<palW && Mouse.getY()>sw*((float) h)/(w-sw) && Mouse.getY()<sw*((float) h)/(w-sw)+palH){
	    		//System.out.println(Mouse.getX()+","+Mouse.getY());
	    		paletteChannel = 2 - (int)(3*(Mouse.getY() - sw*((float) h)/(w-sw)))/(palH);
	    		//System.out.println(paletteChannel);
	    		float mindistsq = 100000000;
	    		palettePoint = -1;
	    		float x = Mouse.getX()/(float)palW;
	    		float y = 3*(Mouse.getY() - sw*((float) h)/(w-sw) - (2-paletteChannel)/3.0f*palH)/(float)palH;
	    		//System.out.println(y);
	    		for (int i=0; i<pals.get(paletteChannel).size(); i++){
	    			float distsq = (pals.get(paletteChannel).get(i)[0]-x)*(pals.get(paletteChannel).get(i)[0]-x) + (pals.get(paletteChannel).get(i)[1]-y)*(pals.get(paletteChannel).get(i)[1]-y);
	    			if (distsq<mindistsq){
	    				mindistsq = distsq;
	    				palettePoint = i;
	    			}
	    		}
	    	} else {
	    		paletteChannel = -1;
	    	}
    	} else {
    		if (palettePoint != 0 && palettePoint != pals.get(paletteChannel).size()-1){
	    		pals.get(paletteChannel).get(palettePoint)[0]+=modifier*mdx/(float)palW;
	    		pals.get(paletteChannel).get(palettePoint)[0] = Math.max(pals.get(paletteChannel).get(palettePoint-1)[0], Math.min(pals.get(paletteChannel).get(palettePoint+1)[0], pals.get(paletteChannel).get(palettePoint)[0]));//clamp
    		}
    		pals.get(paletteChannel).get(palettePoint)[1]+=modifier*mdy*3.0f/palH;
    		pals.get(paletteChannel).get(palettePoint)[1] = Math.max(0, Math.min(1, pals.get(paletteChannel).get(palettePoint)[1]));//clamp
    	}
		if (!paletteDragging && Mouse.isButtonDown(0) && Mouse.getX()>sw && !Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) && !mouseDragging && (mdx!=0 || mdy!=0)){
			//System.out.println("mdx: "+mdx+", mdy: "+mdy);
			fractal.move((float) (-mdx/(h*0.5*fractal.zoom)),(float) (-mdy/(h*0.5*fractal.zoom)));
		}
		if (Mouse.isButtonDown(2) || Mouse.isButtonDown(3)){//middle mouse or first extra mouse button
			if (viewingMandelbrot && Mouse.getX()>sw){
				int dx = Mouse.getX() - (sw/2 + w/2);
				int dy = Mouse.getY() - h/2;
				julia.extraVars[0] = (float) mandelbrot.centerx + (float) (dx/(h*0.5*mandelbrot.zoom));
				julia.extraVars[1] = (float) mandelbrot.centery + (float) (dy/(h*0.5*mandelbrot.zoom));
				julia.changed = true;
			} else if (!viewingMandelbrot && Mouse.getX()<sw && Mouse.getY()<sw*((float) h)/(w-sw)){
				int dx = Mouse.getX() - sw/2;
				int dy = Mouse.getY() - (int) (0.5*sw*((float) h)/(w-sw));
				julia.extraVars[0] = (float) mandelbrot.centerx + (float) (dx/(sw*((float) h)/(w-sw)*0.5*mandelbrot.zoom));
				julia.extraVars[1] = (float) mandelbrot.centery + (float) (dy/(sw*((float) h)/(w-sw)*0.5*mandelbrot.zoom));
				julia.changed = true;
			}
		}
    	while (Mouse.next()){
    		if (Mouse.getEventButton() == 1){//RMB
    			if (Mouse.getEventButtonState()){
	    			if (Mouse.getX() >= sw){
	    				fractal.savePrevState();
	    				mouseDragging = true;
	    				mdownx = Mouse.getX();
	    				mdowny = Mouse.getY();
	    			}
    			} else {
    				if (Mouse.getX() >= sw && mouseDragging){
    					int dx = (sw+w-mdownx-Mouse.getX())/2;
    					int dy = (h-mdowny-Mouse.getY())/2;
    					fractal.move((float) (-dx/(h*0.5*fractal.zoom)),(float) (-dy/(h*0.5*fractal.zoom)));
    					//System.out.println(Math.max((w-sw)/(double)(Mouse.getX()-mdownx), h/(double)(Mouse.getY()-mdowny)));
    					fractal.zoom(Math.min((w-sw)/(double)Math.abs(Mouse.getX()-mdownx), h/(double)Math.abs(Mouse.getY()-mdowny)));
    				}
					mouseDragging = false;
    			}
    		} else if (Mouse.getEventButton() == 0){//LMB
    			if (Mouse.getEventButtonState()){//push
	    			if (Mouse.getX() >= sw && Keyboard.isKeyDown(Keyboard.KEY_LCONTROL)){
	    				fractal.savePrevState();
	    				fractal.move((float) (-(sw/2 + w/2 - Mouse.getX())/((w-sw)*(h/(float)(w-sw))*0.5*fractal.zoom)),(float) (-(h/2 - Mouse.getY())/(h*0.5*fractal.zoom)));
	    				Mouse.setCursorPosition(sw/2 + w/2, h/2);
	    			}
	    			if (Mouse.getX()>0 && Mouse.getX()<palW && Mouse.getY()>sw*((float) h)/(w-sw) && Mouse.getY()<sw*((float) h)/(w-sw)+palH){
	    				paletteDragging = true;
	    			}
    			} else {//release
    				if (paletteDragging){
    					paletteDragging = false;
    					calcPalette();
    				}
    			}
    		} else {
    			int dwheel = Mouse.getEventDWheel();
    			if (dwheel!=0 && Mouse.getX()>sw) fractal.zoomStep(dwheel,Mouse.getX()-sw, Mouse.getY());
    		}
    	}
    	
    	while (Keyboard.next()){
    		if (Keyboard.getEventKeyState()){
    			switch(Keyboard.getEventKey()){
    			case Keyboard.KEY_RETURN:
    				fractal.savePrevState();
    				fractal.changed = true;
    				break;
    			case Keyboard.KEY_ESCAPE:
    				done = true;
    				break;
    			case Keyboard.KEY_BACK:
    				fractal.loadPrevState();
    				break;
    			case Keyboard.KEY_X:
    				fractal.saveImage();
    				break;
    			case Keyboard.KEY_Q:
    				fractal.maxIteration+= modifier*100;
    				break;
    			case Keyboard.KEY_A:
    				fractal.maxIteration-= modifier*100;
    				break;
    			case Keyboard.KEY_W:
    				fractal.transformParam+= modifier*10.0;
    				break;
    			case Keyboard.KEY_S:
    				fractal.transformParam-= modifier*10.0;
    				break;
    			case Keyboard.KEY_J:
    				if (viewingMandelbrot && Mouse.getX()>sw){
    					int dx = Mouse.getX() - (sw/2 + w/2);
    					int dy = Mouse.getY() - h/2;
    					julia.extraVars[0] = (float) mandelbrot.centerx + (float) (dx/(h*0.5*mandelbrot.zoom));
    					julia.extraVars[1] = (float) mandelbrot.centery + (float) (dy/(h*0.5*mandelbrot.zoom));
    					julia.changed = true;
    				} else if (!viewingMandelbrot && Mouse.getX()<sw && Mouse.getY()<sw*((float) h)/(w-sw)){
    					int dx = Mouse.getX() - sw/2;
    					int dy = Mouse.getY() - (int) (0.5*sw*((float) h)/(w-sw));
    					julia.extraVars[0] = (float) mandelbrot.centerx + (float) (dx/(sw*((float) h)/(w-sw)*0.5*mandelbrot.zoom));
    					julia.extraVars[1] = (float) mandelbrot.centery + (float) (dy/(sw*((float) h)/(w-sw)*0.5*mandelbrot.zoom));
    					julia.changed = true;
    				}
    				break;
    			case Keyboard.KEY_TAB:
        			viewingMandelbrot = !viewingMandelbrot;
        			julia.changed = true;
        			mandelbrot.changed = true;
        			break;
    			}
    		}
    	}
    }

    private void init(){
    	lastFPS = getTime();
        try{
            Display.setDisplayMode(new DisplayMode(w, h));
            Display.setVSyncEnabled(true);
            Display.setTitle("Fractal Viewer");
            Display.create();
        }catch(Exception e){
            System.out.println("Error setting up display");
            System.exit(0);
        }

        GL11.glViewport(0,0,w,h);
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadIdentity();
        GLU.gluPerspective(45.0f, ((float)w/(float)h),0.1f,100.0f);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glLoadIdentity();
        GL11.glShadeModel(GL11.GL_SMOOTH);
        GL11.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        GL11.glClearDepth(1.0f);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthFunc(GL11.GL_LEQUAL);
        GL11.glHint(GL11.GL_PERSPECTIVE_CORRECTION_HINT,GL11.GL_NICEST);
        
        Font awtFont = new Font("Default", Font.PLAIN, 14);
    	font = new UnicodeFont(awtFont);
    	font.getEffects().add(new ColorEffect(java.awt.Color.white));
    	font.addAsciiGlyphs();
        try { font.loadGlyphs(); }
        catch (SlickException e) { 
           e.printStackTrace(); 
        }
    }
    
    void calcPalette(){
	    for (int i=0; i<palW; i++){
	    	for (int j=0; j<palH; j++){
	    		float mu = (i+j*palW)/(float)(palW*palH);
		    	//changeTexelColor(palette, i, j, mu, mu, mu, 1.0f);
	    		float[] cvals = {0.0f,0.0f,0.0f,1.0f};
	    		for (int n=0; n<3; n++){
	    			//get the two surrounding points
	    			int pointA = 0, pointB = 0;
	    			while (pals.get(n).get(pointB)[0]<=mu){
	    				pointB++;
	    			}
	    			pointA = pointB - 1;
	    			//then interpolate
	    			cvals[n] = pals.get(n).get(pointA)[1] + (mu-pals.get(n).get(pointA)[0])*((pals.get(n).get(pointB)[1]-pals.get(n).get(pointA)[1])/(pals.get(n).get(pointB)[0]-pals.get(n).get(pointA)[0]));
	    		}
	    		changeTexelColor(palette, i, j, cvals[0], cvals[1], cvals[2], cvals[3]);
		    }
	    }
    }
    
    void changeTexelColor(int id, int x, int y, float r, float g, float b, float a) {
        FloatBuffer data = BufferUtils.createFloatBuffer(4);
        float[] d = {r,g,b,a};
        data.put(d);
        data.flip();
        glBindTexture(GL_TEXTURE_2D, id);
        glTexSubImage2D(GL_TEXTURE_2D,
                        0,
                        x,
                        y,
                        1,
                        1,
                        GL_RGBA,
                        GL_FLOAT,
                        data);
    }
    
    /**
	 * Get the accurate system time
	 * 
	 * @return The system time in milliseconds
	 */
	public long getTime() {
	    return (Sys.getTime() * 1000) / Sys.getTimerResolution();
	}
	
	/**
	 * Calculate the FPS and set it in the title bar
	 */
	public void updateFPS() {
		if (getTime() - lastFPS > 1000) {
			Display.setTitle("FPS: " + fps);
			fps = 0;
			lastFPS += 1000;
		}
		fps++;
	}

    public static void main(String[] args){
        new Main();
    }
}
