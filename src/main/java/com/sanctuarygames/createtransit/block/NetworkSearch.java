package com.sanctuarygames.createtransit.block;
import java.util.*;
import java.util.function.*;
/** No output signal is sampled, so ring-shaped networks cannot latch. */
public final class NetworkSearch {
    public record Result<T>(Set<T> members,boolean powered,boolean overflow) {}
    public static <T> Result<T> scan(T origin,Function<T,List<T>> adjacent,Predicate<T> input,int limit) {
        Set<T> seen=new LinkedHashSet<>();Deque<T> queue=new ArrayDeque<>();seen.add(origin);queue.add(origin);boolean on=false;
        while(!queue.isEmpty()) {
            T next=queue.remove();on|=input.test(next);
            for(T n:adjacent.apply(next))if(!seen.contains(n)) {
                if(seen.size()>=limit)return new Result<>(seen,false,true);
                seen.add(n);queue.add(n);
            }
        }
        return new Result<>(seen,on,false);
    }
}
