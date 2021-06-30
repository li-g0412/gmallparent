package com.atguigu.gmall.payment.controller;

import com.alipay.api.AlipayApiException;
import com.alipay.api.internal.util.AlipaySignature;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.enums.PaymentType;
import com.atguigu.gmall.model.payment.PaymentInfo;
import com.atguigu.gmall.payment.config.AlipayConfig;
import com.atguigu.gmall.payment.service.AlipayService;
import com.atguigu.gmall.payment.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Controller
@RequestMapping("/api/payment/alipay")
public class AlipayController {

    @Autowired
    private AlipayService alipayService;

    @Autowired
    private PaymentService paymentService;

    //  api/payment/alipay/submit/{orderId}
    @RequestMapping("submit/{orderId}")
    @ResponseBody
    public String payOrder(@PathVariable Long orderId){

        //  调用服务层方法
        String form = alipayService.createaliPay(orderId);

        return form;
    }

    //  设置一个回调 callback/return
    @GetMapping("callback/return")
    public String retrunCallback(){
        //  同步回调！ 返回一个给用户看的地址！
        //  redirect: "http://payment.gmall.com/pay/success.html" 支付成功的页面！跟web-all 有关系！
        //  重定向：
        return "redirect:"+ AlipayConfig.return_order_url;
    }

    //  http://3ac3pc.natappfree.cc/api/payment/alipay/callback/notify
    //  https://商家网站通知地址?voucher_detail_list=[{"amount":"0.20","merchantContribute":"0.00","name":"5折券","otherContribute":"0.20","type":"ALIPAY_DISCOUNT_VOUCHER","voucherId":"2016101200073002586200003BQ4"}]&fund_bill_list=[{"amount":"0.80","fundChannel":"ALIPAYACCOUNT"},{"amount":"0.20","fundChannel":"MDISCOUNT"}]&subject=PC网站支付交易&trade_no=2016101221001004580200203978&gmt_create=2016-10-12 21:36:12&notify_type=trade_status_sync&total_amount=1.00&out_trade_no=mobile_rdm862016-10-12213600&invoice_amount=0.80&seller_id=2088201909970555&notify_time=2016-10-12 21:41:23&trade_status=TRADE_SUCCESS&gmt_payment=2016-10-12 21:37:19&receipt_amount=0.80&passback_params=passback_params123&buyer_id=2088102114562585&app_id=2016092101248425&notify_id=7676a2e1e4e737cff30015c4b7b55e3kh6& sign_type=RSA2&buyer_pay_amount=0.80&sign=***&point_amount=0.00
    @PostMapping("callback/notify")
    @ResponseBody
    public String callbackNotify(@RequestParam Map<String, String> paramMap){
        System.out.println("你回来了....");
        //  需要如何处理！https://opendocs.alipay.com/open/270/105902
        //  Map<String, String> paramsMap = ... //将异步通知中收到的所有参数都存放到map中
        boolean signVerified = false; //调用SDK验证签名
        try {
            signVerified = AlipaySignature.rsaCheckV1(paramMap, AlipayConfig.alipay_public_key, AlipayConfig.charset, AlipayConfig.sign_type);
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        //  获取交易状态！
        String tradeStatus = paramMap.get("trade_status");
        //  验证out_trade_no 是否为 商户系统中创建的订单号；
        //  可以使用 out_trade_no 查询交易记录中的数据！
        String outTradeNo = paramMap.get("out_trade_no");
        PaymentInfo paymentInfoQuery = paymentService.getPaymentInfo(outTradeNo, PaymentType.ALIPAY.name());
        if (paymentInfoQuery==null){
            return "failure";
        }
        //   total_amount , seller_id , app_id
        //   String outTradeNo = paramMap.get("total_amount"); paymentInfoQuery.getTotalAmount();
        //   String sellerId = paramMap.get("seller_id");
        //   String appId = paramMap.get("app_id"); naocs 的appId 比较！

        if(signVerified){
            // TODO 验签成功后，按照支付结果异步通知中的描述，对支付结果中的业务内容进行二次校验，校验成功后在response中返回success并继续商户自身业务处理，校验失败返回failure
            // 只有交易通知状态为 TRADE_SUCCESS 或 TRADE_FINISHED 时，支付宝才会认定为买家付款成功
            if ("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus)){
                //  支付成功之后，需要更新交易记录状态！
                //  更新payment_status ， trade_no ，callback_time ，callback_content
                paymentService.paySuccess(outTradeNo,PaymentType.ALIPAY.name(),paramMap);
                return "success";
            }
            return "failure";
        }else{
            // TODO 验签失败则记录异常日志，并在response中返回failure.
            return "failure";
        }
    }

    //  退款数据：
    //  我们需要传递订单Id order_info.id  |  order_info.out_trade_no
    //  http://localhost:8205/api/payment/alipay/refund/223
    @GetMapping("refund/{orderId}")
    @ResponseBody
    public Result refund(@PathVariable Long orderId){
        //  调用服务层
        Boolean flag = alipayService.refund(orderId);
        return Result.ok(flag);
    }

    //  定义一个关闭支付交易的url！
    @GetMapping("closePay/{orderId}")
    @ResponseBody
    public Boolean closePay(@PathVariable Long orderId){
        //  调用服务层方法
        Boolean flag = alipayService.closePay(orderId);
        //  直接返回
        return flag;
    }

    //  编写一个控制器
    @RequestMapping("checkPayment/{orderId}")
    @ResponseBody
    public Boolean checkPayment(@PathVariable Long orderId){
        //  调用服务层方法
        Boolean flag = alipayService.checkPayment(orderId);
        //  直接返回
        return flag;
    }

    //  查询paymentInfo 的控制器！
    @GetMapping("getPaymentInfo/{outTradeNo}")
    @ResponseBody
    public PaymentInfo getPaymentInfo(@PathVariable String outTradeNo){
        PaymentInfo paymentInfo = paymentService.getPaymentInfo(outTradeNo, PaymentType.ALIPAY.name());
        if (null!=paymentInfo){
            return paymentInfo;
        }
        return null;
    }



}


