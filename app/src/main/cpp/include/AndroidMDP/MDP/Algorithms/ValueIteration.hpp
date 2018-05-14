#ifndef ANDROIDMDP_MDP_VALUEITERATION
#define ANDROIDMDP_MDP_VALUEITERATION

#include <AndroidMDP/Util/Logging.hpp>
#include <AndroidMDP/MDP/Types.hpp>
#include <AndroidMDP/MDP/Util.hpp>
#include <AndroidMDP/Util/Probability.hpp>

namespace AndroidMDP
{
    namespace MDP
    {
        class ValueIteration
        {
        public:
            ValueIteration(unsigned horizon, double epsilon = 0.001, ValueFunction v = {Values(), Actions(0)});

            template <typename M, typename = std::enable_if_t<is_model<M>::value>>
            std::tuple<double, ValueFunction, QFunction> operator()(const M &m);

            void setHorizon(unsigned);
            void setEpsilon(double);

        private:
            double epsilon_;
            unsigned horizon_;
            ValueFunction vParameter_;

            ValueFunction v1_;
        };

        template <typename M, typename>
        std::tuple<double, ValueFunction, QFunction> ValueIteration::operator()(const M &model)
        {
            const size_t S = model.getS();
            const size_t A = model.getA();

            const size_t size = vParameter_.values.size();
            if(size != S)
            {
                if(size != 0)
                {
                    MDP_LOGGER(MDP_SEVERITY_WARNING, "Size of starting function invalid, ignoring...");
                }
                v1_ = makeValueFunction(S);
            }
            else
            {
                v1_ = vParameter_;
            }

            const auto &ir = [&]
            {
                if constexpr (is_model_eigen<M>::value) return model.getRewardFunction();
                else return computeImmediateRewards(model);
            }();

            unsigned timestep = 0;
            double variation = epsilon_ * 2;

            Values val0;
            auto &val1 = v1_.values;
            QFunction q = makeQFunction(S, A);

            const bool useEpsilon = Util::checkDifferentSmall(epsilon_, 0.0);
            while(timestep < horizon_ && (!useEpsilon || variation > epsilon_))
            {
                timestep ++;
                MDP_LOGGER(MDP_SEVERITY_DEBUG, "Processing timestep " << timestep);

                val0 = val1;

                val1 *= model.getDiscount();
                q = computeQFunction(model, val1, ir);

                bellmanOperatorInline(q, &v1_);

                if(useEpsilon)
                {
                    variation = (val1 - val0).cwiseAbs().maxCoeff();
                }
            }

            return std::make_tuple(useEpsilon ? variation : 0.0, std::move(v1_), std::move(q));
        }
    }
}

#endif
