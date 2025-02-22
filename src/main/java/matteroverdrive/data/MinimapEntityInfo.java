
package matteroverdrive.data;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;

public class MinimapEntityInfo {
    private boolean isAttacking;
    private int entityID;

    public MinimapEntityInfo() {
    }

    public MinimapEntityInfo(EntityLivingBase entityLivingBase, EntityPlayer player) {
        if (entityLivingBase instanceof EntityLiving && ((EntityLiving) entityLivingBase).getAttackTarget() != null) {
            isAttacking = player.equals(((EntityLiving) entityLivingBase).getAttackTarget());
        }
        entityID = entityLivingBase.getEntityId();
    }

    public static boolean hasInfo(EntityLivingBase entityLivingBase, EntityPlayer player) {
        return entityLivingBase instanceof EntityLiving && ((EntityLiving) entityLivingBase).getAttackTarget() != null && player.equals(((EntityLiving) entityLivingBase).getAttackTarget());
    }

    public MinimapEntityInfo writeToBuffer(ByteBuf buf) {
        buf.writeBoolean(isAttacking);
        buf.writeInt(entityID);
        return this;
    }

    public MinimapEntityInfo readFromBuffer(ByteBuf buf) {
        isAttacking = buf.readBoolean();
        entityID = buf.readInt();
        return this;
    }

    public int getEntityID() {
        return entityID;
    }

    public void setEntityID(int entityID) {
        this.entityID = entityID;
    }

    public boolean isAttacking() {
        return isAttacking;
    }

    public void setIsAttacking(boolean isAttacking) {
        this.isAttacking = isAttacking;
    }
}
