package com.lhr.milk.commodity.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lhr.milk.client.UserFeignClient;
import com.lhr.milk.commodity.mapper.AdvancePaymentMapper;
import com.lhr.milk.commodity.service.AdvancePaymentService;
import com.lhr.milk.commodity.service.CommodityService;
import com.lhr.milk.commodity.service.JoinCartService;
import com.lhr.milk.model.model.commodity.AdvancePayment;
import com.lhr.milk.model.model.user.UserInfo;
import com.lhr.milk.model.vo.AdvancePaymentVo;
import com.lhr.milk.model.vo.OrderQueryVo;
import com.lhr.milk.model.vo.UserJoinListVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
/**
 * @author lhr
 * @Date:2022/5/24 14:51
 * @Version 1.0
 */
@Service
public class AdvancePaymentServiceImpl extends ServiceImpl<AdvancePaymentMapper, AdvancePayment>
        implements AdvancePaymentService {

    @Autowired
    private CommodityService commodityService;

    @Autowired
    private JoinCartService joinCartService;

    @Autowired
    private AdvancePaymentMapper advancePaymentMapper;

    @Autowired
    private UserFeignClient userFeignClient;

    @Override
    public void insert(JSONArray jsonArray,long userId) {
        List<UserJoinListVo> joinCarts = jsonArray.toJavaList(UserJoinListVo.class);
        ArrayList<Integer> idList = new ArrayList<>();
        AtomicReference<Integer> totalPrice = new AtomicReference<>(0);
        AtomicReference<String> detail = new AtomicReference<>("");
        ArrayList<HashMap<String, Object>> hashMaps = new ArrayList<>();
        joinCarts.stream().forEach(cart->{
            HashMap<String, Object> map = new HashMap<>();
            long joinCartId = cart.getId();
            joinCartService.delById(joinCartId);
            idList.add((int) joinCartId);
            map.put("name",cart.getName());
            map.put("types",cart.getParam().get("types"));
            map.put("number",cart.getNumber());
            map.put("price",cart.getPrice());
            map.put("joinCartId",joinCartId);
            totalPrice.set((Integer) cart.getParam().get("totalPrice"));
            hashMaps.add(map);
        });

        AdvancePayment advancePayment = new AdvancePayment();
        advancePayment.setOutTradeNo(System.currentTimeMillis() + ""+ new Random().nextInt(100));
        advancePayment.setJoinCartId(idList);
        advancePayment.setUserId(userId);
        advancePayment.setPrice(totalPrice.get());
        advancePayment.setDetail(hashMaps);

        baseMapper.insert(advancePayment);
    }
    /**
     * ???????????????????????????????????????????????????
     * @param orderQueryVo
     * @return
     */
    @Override
    public List<AdvancePaymentVo> getPageList(OrderQueryVo orderQueryVo) {

        Integer  commodityId = orderQueryVo.getCommodityId();
        String isPay = orderQueryVo.getIsPay();
        String commodityName = null;
        String pay = null;
        String noPay = null;
        if (!StringUtils.isEmpty(isPay)){
            if ("?????????".equals(isPay)){
                pay = "0";
                //???????????????  ??????pay=-1 ????????????pay=0
                orderQueryVo.setPay(pay);
            }else {
                noPay = "0";
                orderQueryVo.setNoPay(noPay);
            }
        }
        if (!StringUtils.isEmpty(commodityId)){
            commodityName = commodityService.getById(commodityId).getName();
            orderQueryVo.setCommodityName(commodityName);
        }
        //??????mapper?????????
        List<AdvancePaymentVo> aList = advancePaymentMapper.selectAll(orderQueryVo);
        aList.stream().forEach(itemVo->{
            this.packageAdvancePaymentVo((itemVo));
        });

        return aList ;
    }
    /**
     *
     * @param pageParam
     * @param orderQueryVo
     * @return
     *  @Override
     *     public IPage<AdvancePayment> getPageList(Page pageParam, OrderQueryVo orderQueryVo) {
     *
     *         String  commodityId = orderQueryVo.getCommodityId();
     *         String isPay = orderQueryVo.getIsPay();
     *         QueryWrapper<AdvancePayment> wrapper = new QueryWrapper<>();
     *         if (!StringUtils.isEmpty(isPay)){
     *             if (!"?????????".equals(isPay)){
     *                 wrapper.eq("payId",0);
     *             }else {
     *                 wrapper.ne("payId",0);
     *             }
     *         }
     *         if (!StringUtils.isEmpty(commodityId)){
     *             Commodity commodity = commodityService.getById(commodityId);
     *             wrapper.in("detail",commodity.getName());
     *         }
     *         //??????mapper?????????
     *         Page<AdvancePayment> pages = baseMapper.selectPage(pageParam, wrapper);
     *         List payments = baseMapper.selectList(wrapper);
     *         System.out.println(payments.get(0).toString());
     *         pages.getRecords().stream().forEach(item->{
     *             //??????detail????????????param
     *             this.packDetail(item);
     *         });
     *         return pages;
     *     }
     *
     *     private void packDetail(AdvancePayment item) {
     *         List<HashMap<String, Object>> detail = item.getDetail();
     *         detail.stream().forEach(chart->{
     *             String name = (String) chart.get("name");
     *             item.getParam().put("name",name);
     *         });
     *     }
     *
     */
    /**
     * ???????????????id??????????????????????????????
     * @param id
     * @return
     */
    @Override
    public void cancel(long id) {
        baseMapper.deleteById(id);
    }
    /**
     * ???????????????id??????????????????????????????
     * @param orderId
     * @return
     */
    @Override
    public AdvancePaymentVo orderDetail(long orderId) {
        AdvancePaymentVo itemVo = advancePaymentMapper.selectByorderId(orderId);
        this.packageAdvancePaymentVo(itemVo);
        UserInfo userinfo = userFeignClient.getUserById(itemVo.getUserId());
        itemVo.setEmail(userinfo.getEmail());
        return itemVo;
    }
    /**
     * ????????????????????????????????????????????????
     * @param itemVo
     */
    public void packageAdvancePaymentVo(AdvancePaymentVo itemVo){
        //??????????????????????????????
        String detail = itemVo.getDetail();
        List<Map> arrayList = new ArrayList<>();
        String[] numberList = detail.substring(0,detail.length()-2).replace("\"","").split("},");
        Arrays.stream(numberList).forEach(list->{
            String[] split = list.split(",");
            HashMap<String, String> map = new HashMap<>();
            String name = split[0].split(":")[1];
            String price = split[1].split(":")[1];
            String types = split[2].split(":")[1];
            String number = split[3].split(":")[1];
            String cart = split[4].split(":")[1];

            map.put("name",name);
            map.put("price",price);
            map.put("types",types);
            map.put("number",number);
            map.put("cart",cart);
            arrayList.add(map);
            itemVo.setParam(arrayList);
            long payId = itemVo.getPayId();
            if (payId==0){
                itemVo.setPayStatus("?????????");
            }else {
                itemVo.setPayStatus("?????????");
            }
        });
    }

}
