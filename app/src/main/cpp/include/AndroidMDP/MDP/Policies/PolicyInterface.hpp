#ifndef ANDROIDMDP_MDP_POLICYINTERFACE_HPP
#define ANDROIDMDP_MDP_POLICYINTERFACE_HPP

#include <AndroidMDP/PolicyInterface.hpp>
#include <AndroidMDP/Types.hpp>

namespace AndroidMDP
{
    namespace MDP
    {
        class PolicyInterface : public virtual AndroidMDP::PolicyInterface<size_t, size_t, size_t>
        {
        public:
            using Base = AndroidMDP::PolicyInterface<size_t, size_t, size_t>;

            virtual Matrix2D getPolicy() const = 0;
        };
    }
}

#endif
