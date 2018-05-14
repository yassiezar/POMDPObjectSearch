#ifndef ANDROIDMDP_MDP_UTIL
#define ANDROIDMDP_MDP_UTIL

#include <stddef.h>
#include <AndroidMDP/MDP/Types.hpp>

namespace AndroidMDP
{
    namespace MDP
    {
        QFunction makeQFunction(size_t, size_t);
        ValueFunction makeValueFunction(size_t);

        void bellmanOperatorInline(const QFunction &q, ValueFunction *v);

        template <typename M, std::enable_if_t<is_model<M>::value, int> = 0>
        Matrix2D computeImmediateRewards(const M &model)
        {
            if constexpr(is_model_eigen<M>::value)
            {
                return model.getRewardFunction();
            }
            else
            {
                const size_t S = model.getS();
                const size_t A = model.getA();

                auto ir = QFunction(S, A);
                ir.fill(0.0);

                for(size_t s = 0; s < S; s ++)
                {
                    for(size_t a = 0; a < A; a ++)
                    {
                        for(size_t s1 = 0; s1 < S; s1 ++)
                        {
                            ir(s, a) += model.getTransitionProbability(s, a, s1) * model.getExpectedReward(s, a, s1);
                        }
                    }
                }
                return ir;
            }
        }

        template<typename M, std::enable_if_t<is_model<M>::value, int> = 0>
        QFunction computeQFunction(const M &model, const Values &v, QFunction ir)
        {
            const size_t A = model.getA();

            if constexpr(is_model_eigen<M>::value)
            {
                for(size_t a = 0; a < A; a ++)
                {
                    ir.col(a).noalias() += model.getTransitionFunction(a) * v;
                }
            }
            else
            {
                const size_t S = model.getS();
                for(size_t s = 0 ; s < S; s ++)
                {
                    for(size_t a = 0; a < A; a ++)
                    {
                        for(size_t s1 = 0; s1 < S; s1 ++)
                        {
                            ir(s, a) += model.getTransitionProbability(s, a, s1) * v[s1];
                        }
                    }
                }
            }
            return ir;
        }
    }
}

#endif
