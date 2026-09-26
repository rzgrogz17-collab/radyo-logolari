package ogzapp.wordgame.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.glutils.PixmapTextureData;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.utils.ScissorStack;


public class ProgressBar extends Group {


    private Rectangle scissors = new Rectangle();
    private Rectangle clipBounds = new Rectangle(0,0,0,0);
    private TextureAtlas.AtlasRegion track;
    private TextureAtlas.AtlasRegion bar;
    private float percent;
    private Color emptyFillColor;
    private Color borderColor;
    private float borderThickness = 2f;
    private NinePatch emptyPatch;
    private NinePatch borderPatch;


    public ProgressBar(TextureAtlas.AtlasRegion track, TextureAtlas.AtlasRegion bar){

        this.track = track;
        this.bar = bar;
        setSize(track.getRegionWidth(), track.getRegionHeight());

        clipBounds.width = 0;
        clipBounds.height = getHeight();
    }


    public void setRoundedTrack(Color emptyFill, Color border, float thickness) {
        this.emptyFillColor = emptyFill;
        this.borderColor = border;
        this.borderThickness = Math.max(1.5f, thickness);
        ensureRoundedPatches();
    }


    private void ensureRoundedPatches() {
        if (emptyPatch != null) return;
        float h = Math.max(8f, getHeight());
        float radius = h * 0.5f;
        emptyPatch = createWhiteRoundedFill(h, radius);
        borderPatch = createWhiteRoundedStroke(h, radius, borderThickness);
    }



    @Override
    public void setX(float x) {
        super.setX(x);
        clipBounds.x = x;
    }



    @Override
    public void setY(float y) {
        super.setY(y);
        clipBounds.y = y;
    }




    public void setPercent(float percent){
        this.percent = percent;
        clipBounds.width = getWidth() * percent;
    }




    public float getPercent(){
        return percent;
    }




    @Override
    public void draw(Batch batch, float parentAlpha) {

        Color color = getColor();
        float alpha = color.a * parentAlpha;

        if (emptyFillColor != null && emptyPatch != null) {
            batch.setColor(emptyFillColor.r, emptyFillColor.g, emptyFillColor.b, emptyFillColor.a * alpha);
            emptyPatch.draw(batch, getX(), getY(), getWidth(), getHeight());
        } else {
            batch.setColor(color.r, color.g, color.b, alpha);
            batch.draw(track, getX(), getY(), getOriginX(), getOriginY(), getWidth(), getHeight(), getScaleX(), getScaleY(), getRotation());
        }
        batch.flush();

        getStage().calculateScissors(clipBounds, this.scissors);

        if (ScissorStack.pushScissors(this.scissors)) {
            batch.setColor(color.r, color.g, color.b, alpha);
            batch.draw(bar, getX(), getY(), getOriginX(), getOriginY(), getWidth(), getHeight(), getScaleX(), getScaleY(), getRotation());

            batch.flush();
            ScissorStack.popScissors();
        }

        if (borderColor != null && borderPatch != null) {
            batch.setColor(borderColor.r, borderColor.g, borderColor.b, borderColor.a * alpha);
            borderPatch.draw(batch, getX(), getY(), getWidth(), getHeight());
            batch.flush();
        }

        batch.setColor(color.r, color.g, color.b, 1);
    }


    private static NinePatch createWhiteRoundedFill(float height, float radius) {
        int h = Math.max(8, Math.round(height));
        int r = Math.max(2, Math.round(radius));
        int stretch = 4;
        int w = r * 2 + stretch;
        Pixmap pixmap = new Pixmap(w, h, Pixmap.Format.RGBA8888);
        pixmap.setBlending(Pixmap.Blending.None);
        pixmap.setColor(0, 0, 0, 0);
        pixmap.fill();
        pixmap.setBlending(Pixmap.Blending.SourceOver);
        fillRoundedRect(pixmap, 0, 0, w, h, r, Color.WHITE);
        return ninePatchFromPixmap(pixmap, r, h);
    }


    private static NinePatch createWhiteRoundedStroke(float height, float radius, float stroke) {
        int h = Math.max(8, Math.round(height));
        int r = Math.max(2, Math.round(radius));
        int s = Math.max(1, Math.round(stroke));
        int stretch = 4;
        int w = r * 2 + stretch;
        Pixmap pixmap = new Pixmap(w, h, Pixmap.Format.RGBA8888);
        pixmap.setBlending(Pixmap.Blending.None);
        pixmap.setColor(0, 0, 0, 0);
        pixmap.fill();
        pixmap.setBlending(Pixmap.Blending.SourceOver);
        fillRoundedRect(pixmap, 0, 0, w, h, r, Color.WHITE);
        pixmap.setBlending(Pixmap.Blending.None);
        pixmap.setColor(0, 0, 0, 0);
        int innerR = Math.max(1, r - s);
        fillRoundedRect(pixmap, s, s, Math.max(1, w - s * 2), Math.max(1, h - s * 2), innerR, new Color(0, 0, 0, 0));
        return ninePatchFromPixmap(pixmap, r, h);
    }


    private static NinePatch ninePatchFromPixmap(Pixmap pixmap, int radius, int height) {
        PixmapTextureData texData = new PixmapTextureData(pixmap, pixmap.getFormat(), false, false, true);
        Texture tex = new Texture(texData);
        tex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        int inset = Math.max(1, radius);
        if (inset * 2 >= height) inset = Math.max(1, height / 2 - 1);
        return new NinePatch(tex, radius, radius, inset, inset);
    }


    private static void fillRoundedRect(Pixmap pixmap, int x, int y, int w, int h, int radius, Color color) {
        if (w <= 0 || h <= 0) return;
        int rad = Math.min(radius, Math.min(w, h) / 2);
        pixmap.setColor(color);
        pixmap.fillRectangle(x + rad, y, Math.max(1, w - rad * 2), h);
        pixmap.fillRectangle(x, y + rad, w, Math.max(1, h - rad * 2));
        pixmap.fillCircle(x + rad, y + rad, rad);
        pixmap.fillCircle(x + w - rad - 1, y + rad, rad);
        pixmap.fillCircle(x + rad, y + h - rad - 1, rad);
        pixmap.fillCircle(x + w - rad - 1, y + h - rad - 1, rad);
    }
}
