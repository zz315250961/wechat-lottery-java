package com.ruoyi.lottery.service;
import org.junit.jupiter.api.Test;import static org.junit.jupiter.api.Assertions.*;
class ImageUploadPolicyTest {private final ImageUploadPolicy p=new ImageUploadPolicy();
 @Test void acceptsRealPngHeader(){assertDoesNotThrow(()->p.validate("gift.png","image/png",new byte[]{(byte)0x89,0x50,0x4e,0x47,0x0d,0x0a,0x1a,0x0a},1024));}
 @Test void rejectsExecutableRenamedAsImage(){assertEquals("图片文件头不合法",assertThrows(IllegalArgumentException.class,()->p.validate("gift.png","image/png",new byte[]{0x4d,0x5a,0x00},3)).getMessage());}
 @Test void rejectsOversizedImage(){assertEquals("图片大小不能超过5MB",assertThrows(IllegalArgumentException.class,()->p.validate("gift.jpg","image/jpeg",new byte[]{(byte)0xff,(byte)0xd8,(byte)0xff},5*1024*1024+1)).getMessage());}
}
