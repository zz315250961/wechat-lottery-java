package com.ruoyi.lottery.service;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class WechatSignatureTest {
 @Test void hashesSignatureSourceWithSha1(){assertEquals("a9993e364706816aba3e25717850c26c9cd0d89d",WechatSignature.sign("abc"));}
 @Test void stripsFragmentFromSignedUrl(){assertEquals("https://example.com/a?x=1",WechatSignature.canonicalUrl("https://example.com/a?x=1#page"));}
}
