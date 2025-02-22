
package matteroverdrive.client.render.tileentity.starmap;

import matteroverdrive.Reference;
import matteroverdrive.client.data.Color;
import matteroverdrive.proxy.ClientProxy;
import matteroverdrive.starmap.data.Galaxy;
import matteroverdrive.starmap.data.Quadrant;
import matteroverdrive.starmap.data.SpaceBody;
import matteroverdrive.tile.TileEntityMachineStarMap;
import matteroverdrive.util.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import static org.lwjgl.opengl.GL11.GL_QUADS;
import static org.lwjgl.opengl.GL11.glLineWidth;

@SideOnly(Side.CLIENT)
public class StarMapRenderGalaxy extends StarMapRendererStars {
    @Override
    public void renderBody(Galaxy galaxy, SpaceBody spaceBody, TileEntityMachineStarMap starMap, float partialTicks, float viewerDistance) {
        double distanceMultiply = 2;
        GlStateManager.depthMask(false);

        glLineWidth(1);

        Tessellator.getInstance().getBuffer().begin(GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
        for (Quadrant quadrant : galaxy.getQuadrants()) {
            renderStars(quadrant, starMap, distanceMultiply, 2);
        }
        Tessellator.getInstance().draw();
        GlStateManager.disableTexture2D();
        GlStateManager.enableTexture2D();
    }

    @Override
    public void renderGUIInfo(Galaxy galaxy, SpaceBody spaceBody, TileEntityMachineStarMap starMap, float partialTicks, float opacity) {
        GlStateManager.enableAlpha();
        int ownedSystemCount = galaxy.getOwnedSystemCount(Minecraft.getMinecraft().player);
        int enemySystemCount = galaxy.getEnemySystemCount(Minecraft.getMinecraft().player);
        int freeSystemCount = galaxy.getStarCount() - ownedSystemCount - enemySystemCount;
        Color color = Reference.COLOR_HOLO_GREEN;
        RenderUtils.applyColorWithMultipy(color, opacity);
        ClientProxy.holoIcons.renderIcon("page_icon_star", 0, -30);
        RenderUtils.drawString(String.format("x%s", ownedSystemCount), 24, -23, color, opacity);

        color = Reference.COLOR_HOLO_RED;
        RenderUtils.applyColorWithMultipy(color, opacity);
        ClientProxy.holoIcons.renderIcon("page_icon_star", 64, -30);
        RenderUtils.drawString(String.format("x%s", enemySystemCount), 88, -23, color, opacity);

        color = Reference.COLOR_HOLO;
        RenderUtils.applyColorWithMultipy(color, opacity);
        ClientProxy.holoIcons.renderIcon("page_icon_star", 128, -30);
        RenderUtils.drawString(String.format("x%s", freeSystemCount), 152, -23, color, opacity);
        GlStateManager.disableAlpha();
    }

    @Override
    public boolean displayOnZoom(int zoom, SpaceBody spaceBody) {
        return true;
    }

    @Override
    public double getHologramHeight(SpaceBody spaceBody) {
        return 2.5;
    }

}
