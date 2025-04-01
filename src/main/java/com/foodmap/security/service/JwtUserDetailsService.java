package com.foodmap.security.service;

import com.foodmap.entity.Shop;
import com.foodmap.entity.User;
import com.foodmap.service.ShopService;
import com.foodmap.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;

@Service
public class JwtUserDetailsService implements UserDetailsService {

    @Autowired
    private UserService userService;

    @Autowired
    private ShopService shopService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 检查是否是用户名
        User user = userService.(username);
        if (user != null) {
            return createUserDetails(user);
        }

        // 检查是否是商铺名
        Shop shop = shopService.username);
        if (shop != null) {
            return createShopDetails(shop);
        }

        throw new UsernameNotFoundException("用户/商铺 " + username + " 不存在");
    }

    // 创建用户的UserDetails
    private UserDetails createUserDetails(User user) {
        Collection<SimpleGrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));

        if (user.getUserType() == 1) {
            authorities.add(new SimpleGrantedAuthority("ROLE_SHOP_ADMIN"));
        }

        return new org.springframework.security.core.userdetails.User(
                user.getUserName(),
                user.getUserPassword(),
                user.getStatus() == 1,  // enabled
                true,  // accountNonExpired
                true,  // credentialsNonExpired
                true,  // accountNonLocked
                authorities
        );
    }

    // 创建商铺的UserDetails
    private UserDetails createShopDetails(Shop shop) {
        Collection<SimpleGrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_SHOP"));

        return new org.springframework.security.core.userdetails.User(
                shop.getShopName(),
                shop.getShopPassword(),  // 确保Shop类中有getPassword方法
                shop.getStatus() == 1,  // enabled
                true,  // accountNonExpired
                true,  // credentialsNonExpired
                true,  // accountNonLocked
                authorities
        );
    }
}